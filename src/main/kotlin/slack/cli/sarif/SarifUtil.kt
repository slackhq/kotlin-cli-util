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
import io.github.detekt.sarif4k.Result
import io.github.detekt.sarif4k.Suppression
import io.github.detekt.sarif4k.SuppressionKind
import java.util.Objects

internal val BASELINE_SUPPRESSION: Suppression = Suppression(
  kind = SuppressionKind.External,
  justification = "This issue was suppressed by the baseline"
)

/**
 * A comparator used to sort instances of the Result class.
 *
 * The comparison is done based on the following properties in the given order:
 * - ruleIndex
 * - ruleID
 * - uri of the first physical location's artifact location
 * - startLine of the first physical location's region
 * - startColumn of the first physical location's region
 * - endLine of the first physical location's region
 * - endColumn of the first physical location's region
 * - text of the message
 */
internal val RESULT_SORT_COMPARATOR =
  compareBy<Result>(
    { it.ruleIndex },
    { it.ruleID },
    { it.locations?.firstOrNull()?.physicalLocation?.artifactLocation?.uri },
    { it.locations?.firstOrNull()?.physicalLocation?.region?.startLine },
    { it.locations?.firstOrNull()?.physicalLocation?.region?.startColumn },
    { it.locations?.firstOrNull()?.physicalLocation?.region?.endLine },
    { it.locations?.firstOrNull()?.physicalLocation?.region?.endColumn },
    { it.message.text },
  )

/**
 * Returns the identity hash code for the [Result] object. This seeks to create a hash code for
 * results that point to the same issue+location, but not necessarily the same
 * [Result.level]/[Result.message].
 */
internal val Result.identityHash: Int
  get() =
    Objects.hash(
      ruleID,
      locations?.firstOrNull()?.physicalLocation?.artifactLocation?.uri,
      locations?.firstOrNull()?.physicalLocation?.region?.startLine,
      locations?.firstOrNull()?.physicalLocation?.region?.startColumn,
      locations?.firstOrNull()?.physicalLocation?.region?.endLine,
      locations?.firstOrNull()?.physicalLocation?.region?.endColumn,
    )

/**
 * Returns the shallow hash code for the [Result] object. This seeks to create a hash code for
 * results that include the [identityHash] but also differentiate if the
 * [Result.level]/[Result.message] are different.
 */
internal val Result.shallowHash: Int
  get() =
    Objects.hash(
      ruleID,
      locations?.firstOrNull()?.physicalLocation?.artifactLocation?.uri,
      locations?.firstOrNull()?.physicalLocation?.region?.startLine,
      locations?.firstOrNull()?.physicalLocation?.region?.startColumn,
      locations?.firstOrNull()?.physicalLocation?.region?.endLine,
      locations?.firstOrNull()?.physicalLocation?.region?.endColumn,
    )

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
