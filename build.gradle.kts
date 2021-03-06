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
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version libs.versions.kotlin.get()
  alias(libs.plugins.dokka)
  alias(libs.plugins.detekt)
  alias(libs.plugins.lint)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
  alias(libs.plugins.binaryCompatibilityValidator)
}

spotless {
  format("misc") {
    target("*.md", ".gitignore")
    trimTrailingWhitespace()
    endWithNewline()
  }
  val ktfmtVersion = libs.versions.ktfmt.get()
  kotlin {
    target("**/*.kt")
    ktfmt(ktfmtVersion).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile("spotless/spotless.kt")
    targetExclude("**/spotless.kt")
  }
  kotlinGradle {
    ktfmt(ktfmtVersion).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile(
      "spotless/spotless.kt",
      "(import|plugins|buildscript|dependencies|pluginManagement)"
    )
  }
}

configure<JavaPluginExtension> { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

tasks.withType<JavaCompile>().configureEach { options.release.set(8) }

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "1.8"
    allWarningsAsErrors = true
    freeCompilerArgs = freeCompilerArgs + listOf("-progressive", "-opt-in=kotlin.RequiresOptIn")
  }
}

tasks.withType<Detekt>().configureEach { jvmTarget = "1.8" }

tasks.withType<DokkaTask>().configureEach {
  outputDirectory.set(rootDir.resolve("../docs/0.x"))
  dokkaSourceSets.configureEach { skipDeprecated.set(true) }
}

kotlin { explicitApi() }

dependencies {
  api(libs.clikt)
  implementation(libs.kotlinShell)
  implementation(libs.okio)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
