import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    `maven-publish`
    kotlin("jvm")
    id("org.danilopianini.git-sensitive-semantic-versioning")
    id("org.danilopianini.publish-on-central")
    id("org.jetbrains.dokka")
    id("io.gitlab.arturbosch.detekt")
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
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:_")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:_")
    implementation("com.uchuhimo:konf:_")
    implementation("io.github.classgraph:classgraph:_")
    implementation("io.arrow-kt:arrow-core:_")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:_")
    testImplementation("io.kotest:kotest-assertions-core-jvm:_")
    runtimeOnly("ch.qos.logback:logback-classic:_")
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

tasks.test {
    useJUnitPlatform()
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