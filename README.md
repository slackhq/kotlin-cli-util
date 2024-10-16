# ⚠️ ARCHIVED

This project has moved to a new home! https://github.com/slackhq/foundry/tree/main/tools/cli

---

# Kotlin CLI Utils

A repo containing basic CLI utilities for Kotlin.

## Installation

[![Maven Central](https://img.shields.io/maven-central/v/com.slack.cli/kotlin-cli-util.svg)](https://mvnrepository.com/artifact/com.slack.cli/kotlin-cli-util)
```gradle
dependencies {
  implementation("com.slack.cli:kotlin-cli-util:<version>")
}
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

## Local testing

If consuming these utilities from a kotlin script file, you can test changes like so:

1. Set the version in `gradle.properties`, such as `2.5.0-LOCAL1`.
2. Run `./gradlew publishToMavenLocal` to publish the current version to your local maven repository.
3. In your script file, add the local repository and update the version:
    ```kotlin
    @file:Repository("file:///Users/{username}/.m2/repository")
    @file:DependsOn("com.slack.cli:kotlin-cli-util:{version you set in gradle.properties}")
    ```
4. Repeat as needed while testing, incrementing the version number each time to avoid caching issues.

License
--------

    Copyright 2022 Slack Technologies, LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[snap]: https://oss.sonatype.org/content/repositories/snapshots/com/slack/cli/
