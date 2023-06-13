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

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class ProcessedExecConfigTest {
  @OptIn(ExperimentalStdlibApi::class)
  @Test
  fun simpleParse() {
    val adapter = ProcessingUtil.newMoshi().adapter<ProcessedExecConfig>()
    // language=json
    val json =
      """
      {
        "version": 1,
        "known_issues": [
          {
            "message": "${KnownIssues.ftlRateLimit.message}",
            "log_message": "${KnownIssues.ftlRateLimit.logMessage}",
            "matching_text": "${KnownIssues.ftlRateLimit.matchingText}",
            "grouping_hash": "${KnownIssues.ftlRateLimit.groupingHash}",
            "retry_signal": {
              "type": "delayed",
              "delay": ${1.minutes.inWholeMilliseconds}
            }
          }
        ]
      }
    """
        .trimIndent()

    val issue = adapter.fromJson(json)!!
    assertThat(issue).isEqualTo(ProcessedExecConfig(1, listOf(KnownIssues.ftlRateLimit)))
  }

  @Test
  fun defaults() {
    val defaultConfig = ProcessedExecConfig()
    assertThat(defaultConfig.knownIssues).isNotEmpty()
  }
}
