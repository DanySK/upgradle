import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    `signing`
    id("org.danilopianini.git-sensitive-semantic-versioning")
    id("org.danilopianini.publish-on-central")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

gitSemVer {
    version = computeGitSemVer()
}

group = "org.danilopianini"
val projectId = "$group.$name"
val fullName = "RefreshVersions aliases"
val projectDetails = "A set of aliases for the refreshVersions Gradle Plugin"

repositories {
    mavenCentral()
    jcenter {
        content {
            onlyForConfigurations(
                "detekt"
            )
        }
    }
}

val jarTask = tasks.named("jar", Jar::class)
val writeJarOutputPath by tasks.registering {
    dependsOn(jarTask)
    val outputFolder = file("$buildDir/jarpath")
    val destination = file("$outputFolder/jarpath")
    outputs.dir(outputFolder)
    doLast {
        destination.writeText(jarTask.get().outputs.files.singleFile.absolutePath)
    }
}

tasks {
    withType<Test> {
        testLogging {
            useJUnitPlatform()
            events("passed", "skipped", "failed", "standardError")
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            showCauses = true
            showStackTraces = true
            events(*org.gradle.api.tasks.testing.logging.TestLogEvent.values())
        }
        dependsOn(writeJarOutputPath)
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:_")
    testImplementation("io.kotest:kotest-assertions-core-jvm:_")
    testImplementation("io.github.classgraph:classgraph:_")
    testImplementation(gradleTestKit())
    testRuntimeOnly(files(writeJarOutputPath))
}

publishOnCentral {
    projectDescription.set(projectDetails)
    projectLongName.set(fullName)
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

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                developers {
                    developer {
                        name.set("Danilo Pianini")
                        email.set("danilo.pianini@gmail.com")
                        url.set("http://www.danilopianini.org/")
                    }
                }
            }
        }
    }
}

if (System.getenv("CI") == true.toString()) {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}
