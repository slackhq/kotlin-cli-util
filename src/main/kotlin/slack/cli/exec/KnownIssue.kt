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

private const val OOM_GROUPING_HASH = "oom"

/**
 * A set of known issues. These names should have semantically meaningful names that can be used to
 * differentiate in Bugsnag reporting (it uses the class name!).
 *
 * Each issue extends [Issue], which has some utilities for checking lines and avoids filling in
 * irrelevant stacktrace elements.
 */
@Suppress("unused") // We look these up reflectively at runtime
internal sealed interface KnownIssue {
  // A simple fake checker for testing this script
  object FakeFailure : Issue("Fake failure", "fake-failure"), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("FAKE FAILURE NOT REAL")) {
        log("Detected fake failure. Beep boop.")
        RetrySignal.Ack(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }

  object FtlRateLimit : Issue("FTL rate limit", "ftl-rate-limit"), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("429 Too Many Requests")) {
        log("Detected FTL rate limit. Retrying in 1 minute.")
        RetrySignal.RetryDelayed(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }

  object Oom : Issue("Generic OOM", OOM_GROUPING_HASH), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("Java heap space")) {
        log("Detected OOM. Retrying immediately.")
        RetrySignal.RetryImmediately(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }

  object FtlInfrastructureFailure :
    Issue("Inconclusive FTL infrastructure failure", "ftl-infrastructure-failure"), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("Infrastructure failure")) {
        log("Detected inconclusive FTL infrastructure failure. Retrying immediately.")
        RetrySignal.RetryImmediately(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }

  object FlankTimeout : Issue("Flank timeout", "flank-timeout"), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("Canceling flank due to timeout")) {
        log("Detected a flank timeout. Retrying immediately.")
        RetrySignal.RetryImmediately(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }

  object R8Oom : Issue("R8 OOM", OOM_GROUPING_HASH), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("Out of space in CodeCache")) {
        log("Detected a OOM in R8. Retrying immediately.")
        RetrySignal.RetryImmediately(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }

  object OomKilledByKernel : Issue("OOM killed by kernel", OOM_GROUPING_HASH), KnownIssue {
    override fun check(lines: List<String>, log: (String) -> Unit): RetrySignal {
      return if (lines.checkContains("Gradle build daemon disappeared unexpectedly")) {
        log("Detected a OOM that was killed by the kernel. Retrying immediately.")
        RetrySignal.RetryImmediately(this)
      } else {
        RetrySignal.Unknown
      }
    }
  }
}
