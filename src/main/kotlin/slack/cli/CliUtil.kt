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
@file:Suppress("unused")

package slack.cli

import java.io.File

/**
 * Skips `build` and cache directories (starting with `.`, like `.gradle`) in [FileTreeWalks][FileTreeWalk].
 */
fun FileTreeWalk.skipBuildAndCacheDirs(): FileTreeWalk {
  return onEnter { dir -> !dir.name.startsWith(".") && dir.name != "build" }
}

/**
 * Filters by a specific [extension].
 */
fun Sequence<File>.filterByExtension(extension: String): Sequence<File> {
  return filter { it.extension == extension }
}

/**
 * Filters by a specific [name].
 */
fun Sequence<File>.filterByName(name: String, withoutExtension: Boolean = true): Sequence<File> {
  return if (withoutExtension) {
    filter { it.nameWithoutExtension == name }
  } else {
    filter { it.name == name }
  }
}

fun List<String>.cleanLineFormatting(): List<String> {
  val cleanedBlankLines = mutableListOf<String>()
  var blankLineCount = 0
  for (newLine in this) {
    if (newLine.isBlank()) {
      if (blankLineCount == 1) {
        // Skip this line
      } else {
        blankLineCount++
        cleanedBlankLines += newLine
      }
    } else {
      blankLineCount = 0
      cleanedBlankLines += newLine
    }
  }

  return cleanedBlankLines.padNewline()
}

private fun List<String>.padNewline(): List<String> {
  val noEmpties = dropLastWhile { it.isBlank() }
  return noEmpties + ""
}
