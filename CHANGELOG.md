Changelog
=========

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
