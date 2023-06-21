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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * An issue that can be reported to Bugsnag.
 *
 * @property message the message shown in the bugsnag report message. Should be human-readable.
 * @property logMessage the message shown in the CI log when [matchingText] is found. Should be
 *   human-readable.
 * @property matchingText the matching text to look for in the log.
 * @property groupingHash grouping hash for reporting to bugsnag. This should usually be unique, but
 *   can also be reused across issues that are part of the same general issue.
 * @property retrySignal the [RetrySignal] to use when this issue is found.
 * @property description an optional description of the issue. Not used in the CLI, just there for
 *   documentation in the config.
 */
@JsonClass(generateAdapter = true)
internal data class Issue(
  val message: String,
  @Json(name = "log_message") val logMessage: String,
  @Json(name = "matching_text") val matchingText: String,
  @Json(name = "grouping_hash") val groupingHash: String,
  @Json(name = "retry_signal") val retrySignal: RetrySignal,
  val description: String? = null,
) {

  private fun List<String>.checkContains(errorText: String): Boolean {
    return any { it.contains(errorText, ignoreCase = true) }
  }

  /** Checks the log for this issue and returns a [RetrySignal] if it should be retried. */
  fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
    return if (lines.checkContains(matchingText)) {
      log(logMessage)
      retrySignal
    } else {
      RetrySignal.Unknown
    }
  }
}

/**
 * Base class for an issue that can be reported to Bugsnag. This is a [Throwable] for BugSnag
 * purposes but doesn't fill in a stacktrace.
 */
internal class IssueThrowable(issue: Issue) : Throwable(issue.message) {

  override fun fillInStackTrace(): Throwable {
    // Do nothing, the stacktrace isn't relevant for these!
    return this
  }
}
