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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.detekt)
  alias(libs.plugins.lint)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
  alias(libs.plugins.binaryCompatibilityValidator)
  alias(libs.plugins.moshix)
  alias(libs.plugins.kotlin.serialization)
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

configure<JavaPluginExtension> { toolchain { languageVersion.set(JavaLanguageVersion.of(20)) } }

tasks.withType<JavaCompile>().configureEach {
  options.release.set(libs.versions.jvmTarget.get().toInt())
}

tasks.withType<Detekt>().configureEach { jvmTarget = libs.versions.jvmTarget.get() }

tasks.withType<DokkaTask>().configureEach {
  outputDirectory.set(rootDir.resolve("../docs/0.x"))
  dokkaSourceSets.configureEach { skipDeprecated.set(true) }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()
}

kotlin {
  explicitApi()
  compilerOptions {
    jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget))
    allWarningsAsErrors.set(true)
    progressiveMode.set(true)
    optIn.add("kotlin.ExperimentalStdlibApi")
  }
}

moshi { enableSealed.set(true) }

dependencies {
  api(libs.clikt)
  implementation(libs.tikxml.htmlEscape)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.xmlutil.serialization)
  implementation(libs.kotlinShell)
  implementation(libs.okio)
  implementation(libs.okhttp)
  implementation(libs.bugsnag)
  implementation(libs.moshi)
  implementation(libs.kotlin.reflect)
  implementation(libs.sarif4k)
  // To silence this stupid log https://www.slf4j.org/codes.html#StaticLoggerBinder
  implementation(libs.slf4jNop)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
