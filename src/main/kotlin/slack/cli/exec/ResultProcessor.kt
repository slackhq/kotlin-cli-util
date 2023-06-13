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

import com.bugsnag.Bugsnag
import com.bugsnag.Report
import com.bugsnag.Severity
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readLines

/**
 * Utility that processes a CI failure log file and optionally signals to retry.
 *
 * ## Processing
 *
 * This processes the failure and logs it to Bugsnag for grouping. This is important for us to try
 * to track and group failures over time. This is not yet implemented.
 *
 * ## Signaling
 *
 * Some CI failures are transient and can be retried. This CLI can signal to retry a CI job by
 * exiting with a specific exit code.
 *
 * Retry signals are
 * - exit 0: nothing to do
 * - exit 1: retry immediately
 * - exit 2: retry in 1 minute
 */
internal class ResultProcessor(
  private val verbose: Boolean,
  private val bugsnagKey: String?,
  private val config: ProcessedExecConfig,
  private val echo: (String) -> Unit,
) {

  fun process(logFile: Path, isAfterRetry: Boolean): RetrySignal {
    echo("Processing CI log from ${logFile.absolutePathString()}")

    val bugsnag: Bugsnag? by lazy { bugsnagKey?.let { key -> createBugsnag(key) } }

    val logLinesReversed = logFile.readLines().asReversed()
    for (issue in config.knownIssues) {
      val retrySignal = issue.check(logLinesReversed, echo)

      if (retrySignal != RetrySignal.Unknown) {
        // Report to bugsnag. Shared common Throwable but with different messages.
        bugsnag?.apply {
          verboseEcho("Reporting to bugsnag: $retrySignal")
          notify(IssueThrowable(issue), Severity.ERROR) { report ->
            // Group by the throwable message
            report.setGroupingHash(issue.groupingHash)
            report.addToTab("Run Info", "After-Retry", isAfterRetry)
            logLinesReversed.parseBuildScan()?.let { scanLink ->
              report.addToTab("Run Info", "Build-Scan", scanLink)
            }
          }
        }

        if (retrySignal is RetrySignal.Ack) {
          echo("Recognized known issue but cannot retry: ${issue.message}")
        } else {
          echo("Found retry signal: $retrySignal")
        }
        return retrySignal
      }
    }

    // TODO some day log these into bugsnag too?
    echo("No actionable items found in ${logFile.name}")
    return RetrySignal.Unknown
  }

  // TODO test this
  private fun List<String>.parseBuildScan(): String? {
    // Find a build scan URL like so
    // Publishing build scan...
    // https://some-server.com/s/ueizlbptdqv6q

    // Index of the publish log. Scan link should be above or below this.
    val indexOfBuildScan = indexOfFirst { it.contains("Publishing build scan...") }
    // Note the lines may be in reverse order here, so try both above and below
    return get(indexOfBuildScan - 1).trim().takeUnless { "https" !in it }
      ?: get(indexOfBuildScan + 1).trim().takeUnless { "https" !in it }
  }

  private fun verboseEcho(message: String) {
    if (verbose) echo(message)
  }

  private fun createBugsnag(key: String): Bugsnag {
    return Bugsnag(key).apply {
      setAutoCaptureSessions(false)
      startSession()

      // Version of this processor for easier tracking of versions.
      setAppVersion("1.0.0")

      // Report synchronously. This is a CLI so we don't care about blocking.
      // Use our own OkHttp based delivery for better reliability and proxy support.
      delivery = OkHttpSyncHttpDelivery

      // Set the app type to the step key. Useful for grouping these by different steps they
      // occur in.
      envOrNull("BUILDKITE_STEP_KEY")?.let { step -> setAppType(step) }

      // Set the release stage based on the branch name. This lets us slice them by "where" in
      // the dev cycle they are occurring.
      envOrNull("BUILDKITE_BRANCH")?.let { branch ->
        val releaseStage =
          when {
            branch == "main" -> "main"
            // Merge queue branch prefixes in aviator and github
            branch.startsWith("mq-") || branch.startsWith("gh-readonly-queue") -> "merge-queue"
            else -> "pull-request"
          }
        setReleaseStage(releaseStage)
      }

      // Add metadata to reports
      addCallback { report ->
        verboseEcho("Adding metadata to report")

        // Tabs with misc build info.
        report.populateDeviceTab()
        report.populateBuildKiteTab()
      }
    }
  }
}

private fun Report.populateDeviceTab() {
  addToTab("Device", "OS-version", System.getProperty("os.version"))
  addToTab("Device", "JRE", System.getProperty("java.version"))
  addToTab("Device", "Kotlin", KotlinVersion.CURRENT.toString())
}

private fun Report.populateBuildKiteTab() {
  envOrNull("BUILDKITE_JOB_ID")?.let { jobId -> addToTab("BuildKite", "Job-ID", jobId) }
  envOrNull("BUILDKITE_BUILD_ID")?.let { addToTab("BuildKite", "ID", it) }
  envOrNull("BUILDKITE_BUILD_URL")?.let { addToTab("BuildKite", "URL", it) }
  envOrNull("BUILDKITE_STEP_KEY")?.let { addToTab("BuildKite", "Step-Key", it) }
  envOrNull("BUILDKITE_COMMAND")?.let { addToTab("BuildKite", "CI-Command", it) }
}

private fun envOrNull(envKey: String) = System.getenv(envKey)?.takeUnless { it.isBlank() }
