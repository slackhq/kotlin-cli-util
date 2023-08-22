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
package slack.cli.shellsentry

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.squareup.moshi.adapter
import kotlin.io.path.createDirectories
import okio.buffer
import okio.source
import org.jetbrains.annotations.TestOnly
import slack.cli.projectDirOption

/**
 * Executes a command with Bugsnag tracing and retries as needed. This CLI is a shim over
 * [ShellSentry].
 *
 * Example:
 * ```
 * $ ./<binary> --bugsnag-key=1234 --verbose --configurationFile config.json ./gradlew build
 * ```
 */
public class ShellSentryCli :
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

  override fun run() {
    if (parseOnly) return

    val moshi = ProcessingUtil.newMoshi()
    val config =
      configurationFile?.let {
        echo("Parsing config file '$it'")
        it.source().buffer().use { source -> moshi.adapter<ShellSentryConfig>().fromJson(source) }
      }
        ?: ShellSentryConfig()

    // Temporary dir for command output
    val cacheDir = projectDir.resolve("tmp/shellsentry")
    cacheDir.createDirectories()

    ShellSentry(
        command = args.joinToString(" "),
        workingDir = projectDir,
        cacheDir = cacheDir,
        config = config,
        verbose = verbose,
        bugsnagKey = bugsnagKey,
        debug = debug,
        noExit = noExit,
        echo = ::echo
      )
      .exec()
  }
}
