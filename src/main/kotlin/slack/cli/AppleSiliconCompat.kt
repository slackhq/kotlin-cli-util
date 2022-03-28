/*
 * Copyright (C) 2022 Slack Technologies, LLC
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
package slack.cli

import eu.jrie.jetbrains.kotlinshell.shell.shell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okio.Buffer

public object AppleSiliconCompat {
  /**
   * Validates that the current process is not running under Rosetta.
   *
   * If the current process is running under Rosetta, it (Java in this case) will think it's
   * running x86 but Rosetta leaves a peephole to check if the current process is running as a
   * translated binary.
   *
   * We do this to ensure that folks are using arm64 JDK builds for native performance.
   *
   * Peephole: https://developer.apple.com/documentation/apple-silicon/about-the-rosetta-translation-environment#Determine-Whether-Your-App-Is-Running-as-a-Translated-Binary
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  public fun validate(
    errorMessage: () -> String
  ) {
    if (System.getenv("SLACK_SKIP_APPLE_SILICON_CHECK")?.toBoolean() == true) {
      // Toe-hold to skip this check if anything goes wrong.
      return
    }

    if (System.getProperty("os.name") != "Mac OS X") {
      // Not a macOS device, move on!
      return
    }

    val arch = System.getProperty("os.arch")
    if (arch == "aarch64") {
      // Already running on an arm64 JDK, we're good
      return
    }

    check(arch == "x86_64") {
      "Unsupported architecture: $arch"
    }

    shell {
      val buffer = Buffer()
      val pipeline = pipeline {
        "sysctl -in sysctl.proc_translated".process() pipe buffer.outputStream()
      }
      pipeline.join()
      val isTranslated = buffer.readUtf8()
      if (isTranslated.trim() == "1") {
        error(errorMessage)
      } else if (isTranslated.trim() != "0") {
        error("Could not determine if Rosetta is running. Please ensure that sysctl is available on your PATH env. It is normally available under /usr/sbin or /sbin.")
      }
    }
  }
}
