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
package slack.cli.sarif

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import io.github.detekt.sarif4k.Level

private val LEVEL_NAMES =
  Level.entries.joinToString(separator = ", ", prefix = "[", postfix = "]", transform = Level::name)

internal fun CliktCommand.levelOption(): NullableOption<Level, Level> {
  return option(
      "--level",
      "-l",
      help = "Priority level. Defaults to Error. Options are $LEVEL_NAMES"
    )
    .enum<Level>()
}
