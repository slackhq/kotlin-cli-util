Changelog
=========

2.6.2
-----

_2024-01-09_

- **Bug Fix**: Use `discinctBy` when deduping sarif results

2.6.1
-----

_2024-01-08_

- **Enhancement**: Mark a number of buildkite APIs as `Keyable` if they can have a `key: String` property.
- Update Clikt to `4.2.2`.
- Update to Kotlin `1.9.22`.

2.6.0
-----

_2023-12-18_

- **New**: Add Buildkite Pipeline bindings under the `slack.cli.buildkite` package. Note this package is subject to API changes as we iterate on it. This is for use with generating dynamic buildkite pipelines.
- **Enhancement**: Introduce more modern `Path` walking APIs with `FileVisitorBuilder.skipBuildAndCacheDirs()`, `Path.walkEachFile()`, `Sequence<Path>.filterByExtension(extension: String)`, and `Sequence<Path>.filterByName(name: String)` extensions.
- Update xmlutil to `0.86.3`.
- Update okio to `3.7.0`.

2.5.4
-----

_2023-12-05_

- **Enhancement**: Validate all directories with build files match settings files in `GradleSettingsVerifierCli`.
- **Enhancement**: Add `ApplyBaselinesToSarifs` CLI for updating or merging sarif results based on a given baseline. This has two modes (see their docs) for use with either a baseline of suppressed issues (i.e. detekt/lint baseline files) or a baseline of the base branch that it's updating from. This will mark the final output with `baselineData` and `suppressions` accordingly.
- **Enhancement**: Mark merged lint baselines as suppressed.
- **Enhancement**: Add a `level` option to lint baseline merging.

2.5.3
-----

_2023-12-01_

- Update kotlinx-serialization to `1.6.2`.
- Add files arg + use path APIs in sarif merging. This allows specifying a variable number of extra files args for manual merging of files.
- Introduce `CommandFactory` to aggregate commands. You can invoke the `runCommand()` function with keys to known CLIs (check their sources for keys or run with no args to print the help details). This makes it easier to invoke any CLI from a single entrypoint.
- Add `messageTemplate` and `level` options to lint baseline merger.
- List issues individually in lint baseline merges + preserve messages.

2.5.2
-----

_2023-11-27_

- **New**: Add `GradleSettingsVerifierCli` for verifying simple settings.gradle files.
- Update to Kotlin `1.9.21`.
- Update to MoshiX `0.25.1`.

2.5.1
-----

_2023-11-22_

- **New**: Add `GradleProjectFlattenerCli` for flattening nested gradle projects to top-level projects.
- Update coroutines to `1.6.1`.

2.5.0
-----

_2023-11-09_

- **Fix**: Strip leading `file://` path in sarif merging when `--remove-uri-prefixes` is specified.
- **Enhancement**: Allow graceful handling of no sarif files when merging via `--allow-empty` flag.
- Update to JVM target 17.
- Update to Kotlin `1.9.20`.
- Update to MoshiX `0.25.0`.

2.4.0
-----

_2023-11-02_

- **New**: Upstream `MergeSarifReports`, a CLI for merging (lint and detekt) sarif reports from project build directories. We use this in our CI to merge all the reports from all the modules into one report.
- **Fix**: Only use relative paths in lint baseline merged sarifs.

2.3.1
-----

_2023-10-31_

- Add `rules` to lint baseline merging output + pretty print.

2.3.0
-----

_2023-10-30_

- Add new `LintBaselineMergerCli` for merging lint baseline files into a sarif output file.
- Improve logging in `ResultProcessor` in ShellSentry.
- Update OkHttp to `4.12.0`.
- Update Bugsnag to `3.7.1`.
- Update Clikt to `4.2.1`.
- Update Okio to `3.6.0`.
- Update MoshiX to `0.24.3`.

2.2.1
-----

_2023-08-24_

- Fix `ShellSentry.create(argv)` putting the `--parse-only` flag in the wrong place.

2.2.0
-----

_2023-08-24_

- **New**: Extract `ShellSentry` program and make the CLI just wrap this.
- **New**: Add `ShellSentryExtension` to allow adding custom checkers (i.e. non-static/not from config.json) to `ShellSentry`. See the doc on its interface for more details.

2.1.0
-----

_2023-08-10_

- **New**: Add `Issue.matching_patterns` to ShellSentry's config. This allows you to specify a list of regexs to match again instead of just text.
- **Enhancement**: Support multiple matching text inputs to ShellSentry's config. This is a JSON-source-compatible change, single-entry inputs will be wrapped in a list.

These technically introduce breaking changes to `Issue`, but we are currently considering this class to be read-only.

2.0.0
-----

_2023-08-09_

- Update to Clikt 4.1.0. This incurs some breaking API changes, this updates a major version to match.
- Rename "ProcessedExec*" APIs to "ShellSentry*", as this is the name we've decided to give it.
- Update Kotlin to `1.9.0`.
- Update Okio to `3.5.0`.

1.2.3
-----

_2023-07-17_

- Fix retried exit codes not being propagated.

1.2.2
-----

_2023-06-27_

- Lowered jvmTarget to 11.

1.2.1
-----

_2023-06-23_

- Fix `ProcessedExecCli` not reading stderr correctly.
- Add more logging controls to processed exec, namely via `--debug` and `--verbose`.

1.2.0
-----

_2023-06-15_

Happy new year!

- Introduce new `ProcessedExecCli` for post-processing and retrying commands.
- Change `projectDirOption` to use `Path` instead of `File`.
- Update all dependencies (Kotlin 1.8.22, Clikt 3.5.2, Okio 3.3.0).

1.1.1
-----

_2022-04-04_

* Fix: Rosetta checks incorrectly assumed `0` responses on x86 when it's actually going to be an empty string.

1.1.0
-----

_2022-03-31_

* New: `AppleSiliconCompat` utilities for Apple Silicon devices, namely to check if a process is running under Rosetta.
* New: `Toml` utilities.

1.0.0
-----

_2022-03-03_

Initial release
