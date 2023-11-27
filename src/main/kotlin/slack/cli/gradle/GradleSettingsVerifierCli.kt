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
package slack.cli.gradle

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import slack.cli.projectDirOption
import kotlin.system.exitProcess

/** A CLI that verifies a given settings file has only valid projects. */
public class GradleSettingsVerifierCli :
  CliktCommand(help = "A CLI that verifies a given settings file has only valid projects.") {

  private val projectDir by projectDirOption()

  private val settingsFile by
    option(
        "--settings-file",
        "-s",
        help =
          "The settings.gradle file to use. Note this file _must_ only have a single, top-level `include()` call " +
            "with vararg project args."
      )
      .path(mustExist = true, canBeDir = false)
      .required()

  @ExperimentalPathApi
  override fun run() {
    val projectPaths =
      settingsFile
        .readText()
        .trim()
        .lines()
        // Filter out commented lines
        .filterNot { it.trimStart().startsWith("//") }
        .joinToString("\n")
        .removePrefix("include(")
        .removeSuffix(")")
        .split(",")

    val errors = mutableListOf<String>()
    @Suppress("LoopWithTooManyJumpStatements")
    for (line in projectPaths) {
      val path = line.trim().removeSurrounding("\"")
      val realPath =
        projectDir.resolve(path.removePrefix(":").removeSuffix(":").replace(":", File.separator))

      fun reportError(message: String, column: Int) {
        errors += buildString {
          append(message)
          appendLine(line)
          appendLine("${" ".repeat(column)}^")
        }
      }

      when {
        path.endsWith(':') -> {
          reportError("Project paths should not end with ':'", line.lastIndexOf(':') - 1)
        }
        !realPath.exists() -> {
          reportError(
            "Project dir '${realPath.relativeTo(projectDir)}' does not exist.",
            line.indexOfFirst { !it.isWhitespace() }
          )
        }
        !realPath.resolve("build.gradle.kts").exists() -> {
          reportError(
            "Project build file '${realPath.relativeTo(projectDir).resolve("build.gradle.kts")}' does not exist.",
            line.indexOfFirst { !it.isWhitespace() }
          )
        }
        !realPath.isDirectory() -> {
          reportError(
            "Expected '$realPath' to be a directory.",
            line.indexOfFirst { !it.isWhitespace() }
          )
        }
      }
    }

    if (errors.isNotEmpty()) {
      echo("Errors found in '${settingsFile.name}'. Please fix or remove these.", err = true)
      echo(errors.joinToString(""), err = true)
      exitProcess(1)
    }
  }
}
