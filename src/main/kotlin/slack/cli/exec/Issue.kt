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

/** Base class for an issue that can be reported to Bugsnag. */
internal abstract class Issue(
  message: String,
  /**
   * Grouping hash for reporting to bugsnag. This should usually be unique, but can also be reused
   * across issues that are part of the same general issue.
   */
  val groupingHash: String,
) : Throwable(message) {

  override fun fillInStackTrace(): Throwable {
    // Do nothing, the stacktrace isn't relevant for these!
    return this
  }

  protected fun List<String>.checkContains(errorText: String): Boolean {
    return any { it.contains(errorText, ignoreCase = true) }
  }

  /** Checks the log for this issue and returns a [RetrySignal] if it should be retried. */
  abstract fun check(lines: List<String>, log: (String) -> Unit): RetrySignal
}
