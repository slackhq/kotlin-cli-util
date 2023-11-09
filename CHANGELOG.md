Changelog
=========

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
