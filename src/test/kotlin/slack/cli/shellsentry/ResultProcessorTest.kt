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

import com.google.common.truth.Truth.assertThat
import kotlin.io.path.readText
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResultProcessorTest {

  @JvmField @Rule val tmpFolder = TemporaryFolder()

  private val logs = ArrayDeque<String>()

  @Test
  fun testExecuteCommand() {
    tmpFolder.newFile("test.txt")
    val tmpDir = tmpFolder.newFolder("tmp/shellsentry")
    val (exitCode, outputFile) =
      executeCommand(tmpFolder.root.toPath(), "ls -1", tmpDir.toPath(), logs::add)
    assertThat(exitCode).isEqualTo(0)

    val expectedOutput =
      """
      test.txt
      tmp
    """
        .trimIndent()

    assertThat(outputFile.readText().trim()).isEqualTo(expectedOutput)

    // Note we use "contains" here because our script may output additional logs
    assertThat(logs.joinToString("\n").trim()).contains(expectedOutput)
  }

  @Test
  fun testExecuteCommandWithStderr() {
    val script =
      """
      #!/bin/bash

      >&2 echo "Error text"
    """
        .trimIndent()
    val scriptFile =
      tmpFolder.newFile("script.sh").apply {
        writeText(script)
        setExecutable(true)
      }
    tmpFolder.newFile("test.txt")
    val tmpDir = tmpFolder.newFolder("tmp/shellsentry")
    val (exitCode, outputFile) =
      executeCommand(tmpFolder.root.toPath(), scriptFile.absolutePath, tmpDir.toPath(), logs::add)
    assertThat(exitCode).isEqualTo(0)

    val expectedOutput =
      """
      Error text
    """
        .trimIndent()

    assertThat(outputFile.readText().trim()).isEqualTo(expectedOutput)

    // Note we use "contains" here because our script may output additional logs
    assertThat(logs.joinToString("\n").trim()).contains(expectedOutput)
  }

  @Test
  fun unknownIssue() {
    val outputFile = tmpFolder.newFile("logs.txt")
    outputFile.writeText(
      """
      [1/2] FAILURE: Build failed with an exception.
      """
        .trimIndent()
        .padWithTestLogs()
    )
    val signal = newProcessor().process(outputFile.toPath(), isAfterRetry = false)
    check(signal is RetrySignal.Unknown)
  }

  @Test
  fun retryDelayed() {
    val outputFile = tmpFolder.newFile("logs.txt")
    outputFile.writeText(
      """
      ${KnownIssues.ftlRateLimit.matchingText}
      """.trimIndent().padWithTestLogs()
    )
    val signal = newProcessor().process(outputFile.toPath(), isAfterRetry = false)
    check(signal is RetrySignal.RetryDelayed)
  }

  @Test
  fun retryImmediately() {
    val outputFile = tmpFolder.newFile("logs.txt")
    outputFile.writeText(
      """
      ${KnownIssues.oom.matchingText}
      """.trimIndent().padWithTestLogs()
    )
    val signal = newProcessor().process(outputFile.toPath(), isAfterRetry = false)
    check(signal is RetrySignal.RetryImmediately)
  }

  @Test
  fun ack() {
    val outputFile = tmpFolder.newFile("logs.txt")
    outputFile.writeText(
      """
      ${KnownIssues.fakeFailure.matchingText}
      """.trimIndent().padWithTestLogs()
    )
    val signal = newProcessor().process(outputFile.toPath(), isAfterRetry = false)
    check(signal is RetrySignal.Ack)
  }

  @Test
  fun matchingPattern_matches() {
    val outputFile = tmpFolder.newFile("logs.txt")
    outputFile.writeText("""
      FAKE_FAILURE_a
      """.trimIndent().padWithTestLogs())
    val signal = newProcessor().process(outputFile.toPath(), isAfterRetry = false)
    check(signal is RetrySignal.Ack)
  }

  @Test
  fun matchingPattern_doesNotMatch() {
    val outputFile = tmpFolder.newFile("logs.txt")
    outputFile.writeText("""
      FAKE_FAILURE-a
      """.trimIndent().padWithTestLogs())
    val signal = newProcessor().process(outputFile.toPath(), isAfterRetry = false)
    check(signal is RetrySignal.Unknown)
  }

  @Test
  fun parseBuildScan() {
    val url = "https://gradle-enterprise.example.com"
    val scanUrl = "$url/s/ueizlbptdqv6q"
    val log =
      """
      Publishing build scan...
      $scanUrl

    """.trimIndent().padWithTestLogs()

    // Assert in both directions they match
    assertThat(log.lines().parseBuildScan(url)).isEqualTo(scanUrl)
    assertThat(log.lines().reversed().parseBuildScan(url)).isEqualTo(scanUrl)
  }

  private fun newProcessor(): ResultProcessor {
    return ResultProcessor(
      verbose = true,
      bugsnagKey = null,
      config = ShellSentryConfig(),
      echo = logs::add
    )
  }

  // Helper to ensure we're parsing logs from within the test output
  private fun String.padWithTestLogs(): String {
    val prefix = (1..10).joinToString("\n") { randomString() }
    val suffix = (1..10).joinToString("\n") { randomString() }
    return "$prefix\n${randomString()}$this${randomString()}\n$suffix"
  }

  private fun randomString(): String {
    return (0..10).map { ('a'..'z').random() }.joinToString("")
  }
}
