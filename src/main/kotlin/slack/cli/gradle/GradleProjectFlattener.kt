package slack.cli.gradle

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.appendLines
import kotlin.io.path.copyToRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import slack.cli.dryRunOption
import slack.cli.projectDirOption
import kotlin.io.path.appendText

/**
 * A CLI that flattens all gradle projects in a given directory to be top level while preserving their original project paths.
 *
 * This is useful for flattening nested projects that use Dokka, which does not currently support easy doc gen for nested projects and end up with colliding names.
 *
 * It's recommended to run `./gradlew clean` first before running this script to minimize work.
 */
public class GradleProjectFlattenerCli :
  CliktCommand(
    help =
      "A CLI that flattens all gradle projects in a given directory to be top level while preserving their original project paths."
  ) {

  private val projectDir by projectDirOption()

  private val settingsFile by
    option(
        "--settings-file",
        "-s",
        help =
          "The settings.gradle file to use. Defaults to settings.gradle.kts in the current directory. Note this file _must_ only have a single, top-level `include()` call with vararg project args."
      )
      .path(mustExist = true, canBeDir = false)
      .defaultLazy { projectDir.resolve("settings.gradle.kts") }

  private val projectDelimiter: String by option().default("--")

  private val dryRun by dryRunOption()

  @ExperimentalPathApi
  override fun run() {
    val projectPaths =
      settingsFile.readText().trim().removePrefix("include(").removeSuffix(")").split(",").map {
        it.trim().removeSurrounding("\"")
      }

    val newPathMapping = mutableMapOf<String, String>()
    for (path in projectPaths) {
      val realPath = projectDir.resolve(path.removePrefix(":").replace(":", File.separator))
      check(realPath.exists()) { "Expected $realPath to exist." }
      check(realPath.isDirectory()) { "Expected $realPath to be a directory." }
      val newPath = projectDir.resolve(path.removePrefix(":").replace(":", projectDelimiter))
      if (newPath == realPath) {
        // Already top-level, move on
        continue
      }
      newPathMapping[path] = newPath.relativeTo(projectDir).toString()
      echo("Flattening $realPath to $newPath")
      if (!dryRun) {
        realPath.copyToRecursively(newPath, followLinks = false, overwrite = false)
      }
    }

    echo("Finished flattening projects. Updating settings file")
    val newPaths =
      projectPaths.mapNotNull { path ->
        // Point at their new paths
        // Example:
        //   project(":libraries:compose-extensions:pull-refresh").projectDir =
        //     file("libraries--compose-extensions--pull-refresh")
        val newPath = newPathMapping[path] ?: return@mapNotNull null
        "project(\"$path\").projectDir = file(\"$newPath\")".also { echo("+  $it") }
      }

    if (!dryRun) {
      settingsFile.appendText("\n\n")
      settingsFile.appendLines(newPaths)
    }
  }
}
