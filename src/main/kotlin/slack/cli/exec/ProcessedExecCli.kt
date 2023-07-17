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
package slack.cli.exec

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.squareup.moshi.adapter
import eu.jrie.jetbrains.kotlinshell.shell.shell
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteRecursively
import kotlin.system.exitProcess
import okio.buffer
import okio.source
import org.jetbrains.annotations.TestOnly
import slack.cli.projectDirOption

/**
 * Executes a command with Bugsnag tracing and retries as needed.
 *
 * Example:
 * ```
 * $ ./<binary> --bugsnag-key=1234 --verbose --configurationFile config.json ./gradlew build
 * ```
 */
public class ProcessedExecCli :
  CliktCommand("Executes a command with Bugsnag tracing and retries as needed.") {

  internal val projectDir by projectDirOption()
  internal val verbose by option("--verbose", "-v").flag()
  internal val bugsnagKey by option("--bugsnag-key", envvar = "PE_BUGSNAG_KEY")
  internal val configurationFile by
    option("--config", envvar = "PE_CONFIGURATION_FILE")
      .path(mustExist = true, canBeFile = true, canBeDir = false)

  private val debug by option("--debug", "-d").flag()

  @get:TestOnly
  private val noExit by
    option(
        "--no-exit",
        help = "Instructs this CLI to not exit the process with the status code. Test only!"
      )
      .flag()
  @get:TestOnly internal val parseOnly by option("--parse-only").flag(default = false)

  internal val args by argument().multiple()

  @Suppress("CyclomaticComplexMethod")
  @OptIn(ExperimentalStdlibApi::class, ExperimentalPathApi::class)
  override fun run() {
    if (parseOnly) return

    val moshi = ProcessingUtil.newMoshi()
    val config =
      configurationFile?.let {
        echo("Parsing config file '$it'")
        it.source().buffer().use { source -> moshi.adapter<ProcessedExecConfig>().fromJson(source) }
      }
        ?: ProcessedExecConfig()
    // The command to be executed
    val cmd = args.joinToString(" ")

    // Temporary file for command output
    val tmpDir = projectDir.resolve("tmp/processed_exec")
    tmpDir.createDirectories()

    // Initial command execution
    val (initialExitCode, initialLogFile) = executeCommand(cmd, tmpDir)
    var exitCode = initialExitCode
    var logFile = initialLogFile
    var attempts = 0
    while (exitCode != 0 && attempts < 1) {
      attempts++
      echo(
        "Command failed with exit code $exitCode. Running processor script (attempt $attempts)..."
      )

      echo("Processing CI failure")
      val resultProcessor = ResultProcessor(verbose, bugsnagKey, config, ::echo)

      when (val retrySignal = resultProcessor.process(logFile, false)) {
        is RetrySignal.Ack,
        RetrySignal.Unknown -> {
          echo("Processor exited with 0, exiting with original exit code...")
          break
        }
        is RetrySignal.RetryDelayed -> {
          echo(
            "Processor script exited with 2, rerunning the command after ${retrySignal.delay}..."
          )
          // TODO add option to reclaim memory?
          Thread.sleep(retrySignal.delay.inWholeMilliseconds)
          val secondResult = executeCommand(cmd, tmpDir)
          exitCode = secondResult.exitCode
          logFile = secondResult.outputFile
          if (secondResult.exitCode != 0) {
            // Process the second failure, then bounce out
            resultProcessor.process(secondResult.outputFile, isAfterRetry = true)
          }
        }
        is RetrySignal.RetryImmediately -> {
          echo("Processor script exited with 1, rerunning the command immediately...")
          // TODO add option to reclaim memory?
          val secondResult = executeCommand(cmd, tmpDir)
          exitCode = secondResult.exitCode
          logFile = secondResult.outputFile
          if (secondResult.exitCode != 0) {
            // Process the second failure, then bounce out
            resultProcessor.process(secondResult.outputFile, isAfterRetry = true)
          }
        }
      }
    }

    // If we got here, all is well
    // Delete the tmp files
    if (!debug) {
      tmpDir.deleteRecursively()
    }

    echo("Exiting with code $exitCode")
    if (!noExit) {
      exitProcess(exitCode)
    }
  }

  // Function to execute command and capture output. Shorthand to the testable top-level function.
  private fun executeCommand(command: String, tmpDir: Path) =
    executeCommand(projectDir, command, tmpDir, ::echo)
}

internal data class ProcessResult(val exitCode: Int, val outputFile: Path)

// Function to execute command and capture output
internal fun executeCommand(
  workingDir: Path,
  command: String,
  tmpDir: Path,
  echo: (String) -> Unit,
): ProcessResult {
  echo("Running command: '$command'")

  val tmpFile = createTempFile(tmpDir, "processed_exec", ".txt").toAbsolutePath()

  var exitCode = 0
  shell {
    // Weird but the only way to set the working dir
    shell(dir = workingDir.toFile()) {
      // Read the output of the process and write to both stdout and file
      // This makes it behave a bit like tee.
      val echoHandler = stringLambda { line ->
        // The line always includes a trailing newline, but we don't need that
        echo(line.removeSuffix("\n"))
        // Pass the line through unmodified
        line to ""
      }
      val process = command.process() forkErr { it pipe echoHandler pipe tmpFile.toFile() }
      pipeline { process pipe echoHandler pipe tmpFile.toFile() }.join()
      exitCode = process.process.pcb.exitCode
    }
  }

  return ProcessResult(exitCode, tmpFile)
}
