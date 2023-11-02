package slack.cli.sarif

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.SarifSerializer
import java.io.File
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import slack.cli.projectDirOption
import slack.cli.skipBuildAndCacheDirs

public class MergeSarifReports :
  CliktCommand(
    help = "Merges all matching sarif reports into a single file for ease of uploading."
  ) {

  private val projectDir by projectDirOption()
  private val outputFile: File by option("--output-file").file().required()
  private val filePrefix by option("--file-prefix").required()
  private val verbose by option("--verbose", "-v").flag()
  private val remapSrcRoots by
  option(
    "--remap-src-roots",
    help =
    "When enabled, remaps uri roots to include the subproject path (relative to the root project)."
  )
    .flag()
  private val removeUriPrefixes by
  option(
    "--remove-uri-prefixes",
    help =
    "When enabled, removes the root project directory from location uris such that they are only relative to the root project dir."
  )
    .flag()

  private fun log(message: String) {
    if (verbose) {
      echo(message)
    }
  }

  private fun prepareOutput() {
    if (outputFile.exists()) {
      outputFile.delete()
    }
    outputFile.parentFile?.mkdirs()
  }

  private fun findBuildFiles(): List<File> {
    log("Finding build files in ${projectDir.toFile().canonicalFile}")
    val buildFiles =
      projectDir
        .toFile()
        .canonicalFile
        .walkTopDown()
        .skipBuildAndCacheDirs()
        .filter { it.name == "build.gradle.kts" }
        .toList()
    log("${buildFiles.size} build files found")
    return buildFiles
  }

  private fun String.prefixPathWith(prefix: String) = "$prefix/$this"

  private fun findSarifFiles(): List<File> {
    // Find build files first, this gives us an easy hook to then go looking in build/reports dirs.
    // Otherwise we don't have a way to easily exclude populated build dirs that would take forever.
    val buildFiles = findBuildFiles()

    log("Finding sarif files")
    return buildFiles
      .asSequence()
      .flatMap { buildFile ->
        val reportsDir = File(buildFile.parentFile, "build/reports")
        if (reportsDir.exists()) {
          reportsDir.walkTopDown().filter {
            it.isFile && it.extension == "sarif" && it.nameWithoutExtension.startsWith(filePrefix)
          }
        } else {
          emptySequence()
        }
      }
      .toList()
  }

  /**
   * Remaps the srcRoots in the sarif file to be relative to the _root_ project root.
   *
   * This is necessary because, when running lint on a standalone module, the source roots are
   * relative to that _module_'s directory. However, we want the merged sarif to be relative to the
   * root project directory.
   *
   * This function performs that in two steps:
   * 1. Remap the `Run.originalUriBaseIds.%SRCROOT%` to be the root project dir ([projectDir]).
   * 2. Remap the `Result.locations.physicalLocation.artifactLocation.uri` to be relative to the
   *    root project dir by prefixing the path with the module name.
   *
   * ## Example
   *
   * If we have a project with the following structure:
   * ```
   * apps/app-core/src/main/java/com/example/app/MainActivity.kt
   * libraries/lib/src/main/java/com/example/app/LibActivity.kt
   * ```
   *
   * The standard sarif report would denote paths like this:
   * ```
   * src/main/java/com/example/app/MainActivity.kt
   * src/main/java/com/example/app/LibActivity.kt
   * ```
   *
   * And what we actually want is this:
   * ```
   * apps/app-core/src/main/java/com/example/app/MainActivity.kt
   * libraries/lib/src/main/java/com/example/app/LibActivity.kt
   * ```
   */
  private fun SarifSchema210.remapSrcRoots(sarifFile: File): SarifSchema210 {
    //   <module>/─────────────────────────────────┐
    //       build/─────────────────────┐          │
    //          reports/──────┐         │          │
    //                        ▼         ▼          ▼
    val module = sarifFile.parentFile.parentFile.parentFile
    check(File(module, "build.gradle.kts").exists()) {
      "Expected to find build.gradle.kts in $module"
    }
    val modulePrefix = module.toRelativeString(projectDir.toFile())
    return copy(
      runs =
      runs.map { run ->
        val originalUri = run.originalURIBaseIDS!!.getValue(SRC_ROOT)
        run.copy(
          // Remap "%SRCROOT%" to be the root project dir.
          originalURIBaseIDS =
          mapOf(
            SRC_ROOT to
              originalUri.copy(
                uri = "file://${projectDir
                  .toFile().canonicalPath}/"
              )
          ),
          // Remap results to add the module prefix to the artifactLocation.uri.
          results =
          run.results?.map { result ->
            result.copy(
              locations =
              result.locations?.map { location ->
                location.copy(
                  physicalLocation =
                  location.physicalLocation?.copy(
                    artifactLocation =
                    location.physicalLocation
                      ?.artifactLocation
                      ?.copy(
                        uri =
                        location.physicalLocation
                          ?.artifactLocation
                          ?.uri
                          ?.prefixPathWith(modulePrefix)
                      )
                  )
                )
              }
            )
          }
        )
      }
    )
  }

  /** Removes the [projectDir] prefix from location URIs */
  private fun SarifSchema210.remapUris(): SarifSchema210 {
    return copy(
      runs =
      runs.map { run ->
        run.copy(
          results =
          run.results?.map { result ->
            result.copy(
              locations =
              result.locations?.map { location ->
                location.copy(
                  physicalLocation =
                  location.physicalLocation?.let { physicalLocation ->
                    physicalLocation.copy(
                      artifactLocation =
                      physicalLocation.artifactLocation?.let { artifactLocation ->
                        artifactLocation.copy(
                          uri =
                          artifactLocation.uri
                            ?.removePrefix(projectDir.absolutePathString())
                            ?.removePrefix("/")
                        )
                      }
                    )
                  }
                )
              }
            )
          }
        )
      }
    )
  }

  private fun merge(inputs: List<File>) {
    log("Parsing ${inputs.size} sarif files")
    val sarifs =
      inputs.map { sarifFile ->
        log("Parsing $sarifFile")
        val parsed = SarifSerializer.fromJson(sarifFile.readText())
        if (parsed.runs.isEmpty() || parsed.runs[0].results.orEmpty().isEmpty()) {
          return@map parsed
        }
        if (remapSrcRoots) {
          parsed.remapSrcRoots(sarifFile)
        } else if (removeUriPrefixes) {
          parsed.remapUris()
        } else {
          parsed
        }
      }

    log("Merging ${inputs.size} sarif files")
    val sortedMergedRules =
      sarifs
        .flatMap { it.runs.single().tool.driver.rules.orEmpty() }
        .associateBy { it.id }
        .toSortedMap()
    val mergedResults =
      sarifs
        .flatMap { it.runs.single().results.orEmpty() }
        // Some projects produce multiple reports for different variants, so we need to
        // de-dupe.
        .distinct()
        .also { echo("Merged ${it.size} results") }

    // Update rule.ruleIndex to match the index in rulesToAdd
    val ruleIndicesById =
      sortedMergedRules.entries.withIndex().associate { (index, entry) -> entry.key to index }
    val correctedResults =
      mergedResults
        .map { result ->
          val ruleId = result.ruleID
          val ruleIndex = ruleIndicesById.getValue(ruleId)
          result.copy(ruleIndex = ruleIndex.toLong())
        }
        .sortedWith(
          compareBy(
            { it.ruleIndex },
            { it.ruleID },
            { it.locations?.firstOrNull()?.physicalLocation?.artifactLocation?.uri },
            { it.locations?.firstOrNull()?.physicalLocation?.region?.startLine },
            { it.locations?.firstOrNull()?.physicalLocation?.region?.startColumn },
            { it.locations?.firstOrNull()?.physicalLocation?.region?.endLine },
            { it.locations?.firstOrNull()?.physicalLocation?.region?.endColumn },
            { it.message.text },
          )
        )

    val sarifToUse =
      if (removeUriPrefixes) {
        // Just use the first if we don't care about originalUriBaseIDs
        sarifs.first()
      } else {
        // Pick a sarif file to use as the base for the merged sarif file. We want one that has an
        // `originalURIBaseIDS` too since parsing possibly uses this.
        sarifs.find { it.runs.firstOrNull()?.originalURIBaseIDS?.isNotEmpty() == true }
          ?: error("No sarif files had originalURIBaseIDS set, can't merge")
      }

    // Note: we don't sort these results by anything currently (location, etc), but maybe some day
    // we should if it matters for caching
    val runToCopy = sarifToUse.runs.single()
    val mergedTool =
      runToCopy.tool.copy(
        driver = runToCopy.tool.driver.copy(rules = sortedMergedRules.values.toList())
      )

    val mergedSarif =
      sarifToUse.copy(runs = listOf(runToCopy.copy(tool = mergedTool, results = correctedResults)))

    log("Writing merged sarif to $outputFile")
    prepareOutput()
    outputFile.writeText(SarifSerializer.toJson(mergedSarif))
  }

  override fun run() {
    val sarifFiles = findSarifFiles()
    if (sarifFiles.isEmpty()) {
      log("No sarif files found! Did you run lint/detekt first?")
      exitProcess(1)
    }
    merge(sarifFiles)
  }

  private companion object {
    private const val SRC_ROOT = "%SRCROOT%"
  }
}