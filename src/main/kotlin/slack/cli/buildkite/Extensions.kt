package slack.cli.buildkite

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public fun artifacts(vararg artifactPaths: String): SimpleStringValue {
  val paths = artifactPaths.toList()
  return when (paths.size) {
    0 -> SimpleStringValue(emptyList())
    1 -> SimpleStringValue(paths[0])
    else -> SimpleStringValue(paths)
  }
}

public fun SimpleStringValue.coalesceToList(): List<String> = when (this) {
  is SimpleStringValue.ListValue -> value
  is SimpleStringValue.SingleValue -> listOf(value)
}

public fun CommandStep.withAddedArtifacts(vararg newPaths: String): CommandStep {
  val current = artifactPaths?.coalesceToList().orEmpty()
  val new = newPaths.toList()
  return copy(artifactPaths = SimpleStringValue((current + new).distinct()))
}

public fun envMap(vararg env: Pair<String, String>): JsonObject {
  return JsonObject(
    buildMap {
      for ((key, value) in env) {
        put(key, JsonPrimitive(value))
      }
    }
  )
}

public fun CommandStep.withAddedEnv(
  vararg newEnv: Pair<String, String>
): CommandStep {
  return copy(
    env = JsonObject(
      buildMap {
        for ((key, value) in env?.entries.orEmpty()) {
          put(key, value)
        }
        for ((key, value) in newEnv) {
          put(key, JsonPrimitive(value))
        }
      }
    )
  )
}


public object Conditions {
  public const val NOT_CANCELLING: String = "build.state != \"canceling\""
}

public fun githubStatusNotif(context: String, notifyIf: String = Conditions.NOT_CANCELLING): Notification =
  Notification(
    ExternalNotification(
      githubCommitStatus = GithubCommitStatus(context = context),
      notifyIf = notifyIf
    )
  )

public fun CommandStep.withGithubStatus(context: String): CommandStep {
  return copy(
    notify = listOf(githubStatusNotif(context)),
  )
}