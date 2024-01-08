package slack.cli.buildkite

/**
 * A buildkite element that can be identified with a [key].
 */
public interface Keyable {
  public val key: String?
}