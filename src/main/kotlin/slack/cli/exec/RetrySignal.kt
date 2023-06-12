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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal sealed interface RetrySignal {
  val issue: Issue

  /** Unknown issue. */
  object Unknown : RetrySignal {
    override val issue: Issue
      get() = throw IllegalStateException("No issue for RetrySignal.Unknown")
  }

  /** Indicates an issue that is recognized but cannot be retried. */
  data class Ack(override val issue: Issue) : RetrySignal

  /** Indicates this issue should be retried immediately. */
  data class RetryImmediately(override val issue: Issue) : RetrySignal

  /** Indicates this issue should be retried after a [delay]. */
  data class RetryDelayed(override val issue: Issue, val delay: Duration = 1.minutes) : RetrySignal
}
