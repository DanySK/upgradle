import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    `maven-publish`
    kotlin("jvm") version "1.3.70"
    id("org.danilopianini.git-sensitive-semantic-versioning") version "0.2.2"
    id("org.danilopianini.publish-on-central") version "0.2.3"
    id("org.jetbrains.dokka") version "0.10.1"
//    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("io.gitlab.arturbosch.detekt") version "1.7.0"
}

repositories {
    mavenCentral()
    jcenter {
        content {
            includeGroup("org.jetbrains.kotlinx")
        }
    }
}

gitSemVer {
    version = computeGitSemVer()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.7.0")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")
//    implementation("org.eclipse.jgit:org.eclipse.jgit:+")
    implementation("com.uchuhimo:konf:0.22.1")
    implementation("io.github.classgraph:classgraph:4.8.65")
    implementation("io.arrow-kt:arrow-core:0.10.4")
    implementation(kotlin("stdlib-jdk8"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

group = "org.danilopianini" // This must be configured for the generated pom.xml to work correctly
publishOnCentral {
    projectDescription.set("A bot meant to pack updates and send pull requests. Much like dependabot, but way more homemade and focused on Gradle.")
    projectLongName.set("UpGradle")
    licenseName.set("GPL")
    licenseUrl.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
}

detekt {
    failFast = true
    buildUponDefaultConfig = true
    config = files("$projectDir/config/detekt.yml")
    reports {
        html.enabled = true // observe findings in your browser with structure and code snippets
        xml.enabled = true // checkstyle like format mainly for integrations like Jenkins
        txt.enabled = true // similar to the console output, contains issue signature to manually edit baseline files
    }
}