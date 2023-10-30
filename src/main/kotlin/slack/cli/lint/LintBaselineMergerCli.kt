/*
 * Copyright (C) 2023 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slack.cli.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.tickaroo.tikxml.converter.htmlescape.StringEscapeUtils
import io.github.detekt.sarif4k.ArtifactContent
import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Level
import io.github.detekt.sarif4k.Location
import io.github.detekt.sarif4k.Message
import io.github.detekt.sarif4k.PhysicalLocation
import io.github.detekt.sarif4k.Region
import io.github.detekt.sarif4k.Result
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import slack.cli.projectDirOption
import slack.cli.skipBuildAndCacheDirs

/** A CLI that merges lint baseline xml files into one. */
public class LintBaselineMergerCli : CliktCommand("Merges multiple lint baselines into one") {

  private val projectDir by projectDirOption()

  private val baselineFileName by option("--baseline-file-name").required()

  private val outputFile by option("--output-file", "-o").path(canBeDir = false).required()

  private val verbose by option("--verbose", "-v").flag()

  override fun run() {
    val xml = XML { defaultPolicy { ignoreUnknownChildren() } }
    val issues = mutableMapOf<LintIssues.LintIssue, Path>()
    projectDir
      .toFile()
      .walkTopDown()
      .skipBuildAndCacheDirs()
      .map { it.toPath() }
      .filter { it.name == baselineFileName }
      .forEach { file ->
        if (verbose) println("Parsing $file")
        val lintIssues = xml.decodeFromString(serializer<LintIssues>(), file.readText())
        for (issue in lintIssues.issues) {
          if (verbose) println("Parsed $issue")
          issues[issue] = file.parent
        }
      }

    if (verbose) println("Merging ${issues.size} issues")
    val idsToLocations =
      issues.entries
        .groupBy { (issue, _) -> issue.id }
        .mapValues { (_, entries) ->
          entries.map { (issue, projectPath) -> issue.toLocation(projectPath) }
        }
        .toSortedMap()

    if (verbose) println("Writing to $outputFile")
    outputFile.deleteIfExists()
    outputFile.createParentDirectories()
    outputFile.createFile()
    val outputSarif =
      SarifSchema210(
        version = Version.The210,
        runs =
          listOf(
            Run(
              tool = Tool(ToolComponent(name = "lint")),
              results =
                buildList {
                  for ((id, locations) in idsToLocations) {
                    add(
                      Result(
                        ruleID = id,
                        level = Level.Error,
                        locations =
                          locations.sortedBy { it.physicalLocation?.artifactLocation?.uri },
                        message = Message(text = "Lint issue $id")
                      )
                    )
                  }
                }
            )
          )
      )
    Json.encodeToString(SarifSchema210.serializer(), outputSarif).let { outputFile.writeText(it) }
  }

  /**
   * ```
   * <issues format="6" by="lint 8.2.0-alpha10" type="baseline" client="cli" dependencies="false"
   *   name="AGP (8.1.2)" variant="all" version="8.2.0-alpha10">
   *     <issue
   *         id="DoNotMockDataClass"
   *         message="&apos;slack.model.account.Account&apos; is a data class, so mocking it should not be necessary"
   *         errorLine1="  @Mock private lateinit var mockAccount1a: Account"
   *         errorLine2="  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">
   *         <location
   *             file="src/test/java/slack/app/di/CachedOrgComponentProviderTest.kt"
   *             line="46"
   *             column="3"/>
   *     </issue>
   * ```
   */
  @Serializable
  @XmlSerialName("issues")
  internal data class LintIssues(
    val format: Int,
    @Serializable(HtmlEscapeStringSerializer::class) val by: String,
    @Serializable(HtmlEscapeStringSerializer::class) val type: String,
    @Serializable(HtmlEscapeStringSerializer::class) val client: String,
    val dependencies: Boolean,
    @Serializable(HtmlEscapeStringSerializer::class) val name: String,
    @Serializable(HtmlEscapeStringSerializer::class) val variant: String,
    @Serializable(HtmlEscapeStringSerializer::class) val version: String,
    val issues: List<LintIssue>
  ) {
    @Serializable
    @XmlSerialName("issue")
    data class LintIssue(
      val id: String,
      @Serializable(HtmlEscapeStringSerializer::class) val message: String,
      @Serializable(HtmlEscapeStringSerializer::class) val errorLine1: String,
      @Serializable(HtmlEscapeStringSerializer::class) val errorLine2: String,
      val location: LintLocation,
    ) {
      @Serializable
      @XmlSerialName("location")
      data class LintLocation(
        @Serializable(HtmlEscapeStringSerializer::class) val file: String,
        val line: Long?,
        val column: Long?,
      )
    }
  }

  /**
   * A String TypeConverter that escapes and unescapes HTML characters directly from string. This
   * one uses apache 3 StringEscapeUtils borrowed from tikxml.
   */
  internal object HtmlEscapeStringSerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("EscapedString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
      return StringEscapeUtils.unescapeHtml4(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: String) {
      encoder.encodeString(StringEscapeUtils.escapeHtml4(value))
    }
  }

  private fun LintIssues.LintIssue.toLocation(projectPath: Path): Location {
    val uri = projectPath.resolve(location.file).relativeTo(projectDir).absolutePathString()
    return Location(
      physicalLocation =
        PhysicalLocation(
          artifactLocation = ArtifactLocation(uri = uri),
          region =
            Region(
              startLine = location.line,
              startColumn = location.column,
              snippet =
                ArtifactContent(
                  text =
                    """
                      $errorLine1
                      $errorLine2
                    """
                      .trimIndent()
                )
            )
        )
    )
  }
}