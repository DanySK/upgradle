import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import org.danilopianini.gradle.mavencentral.JavadocJar
import org.danilopianini.gradle.mavencentral.SourcesJar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    jacoco
    kotlin("jvm")
    `maven-publish`
    signing
    id("io.gitlab.arturbosch.detekt")
    id("org.danilopianini.git-sensitive-semantic-versioning")
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.danilopianini.publish-on-central")
    id("com.github.johnrengelman.shadow")
    id("com.github.breadmoirai.github-release")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.eclipse.org/content/repositories/egit-releases/")
        content { includeGroup("org.eclipse.mylyn.github") }
    }
    mapOf(
        "kotlin/dokka" to setOf("org.jetbrains.dokka"),
        "kotlin/kotlinx.html" to setOf("org.jetbrains.kotlinx"),
        "arturbosch/code-analysis" to setOf("io.gitlab.arturbosch.detekt")
    ).forEach { (uriPart, groups) ->
        maven {
            url = uri("https://dl.bintray.com/$uriPart")
            content { groups.forEach { includeGroup(it) } }
        }
    }
}

gitSemVer {
    version = computeGitSemVer()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:_")

    implementation("com.github.kittinunf.fuel:fuel:_")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:_")
    implementation("com.github.kittinunf.fuel:fuel-gson:_")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:_")
    implementation("com.uchuhimo:konf:_")
    implementation("io.github.classgraph:classgraph:_")
    implementation("io.arrow-kt:arrow-core:_")
    implementation(kotlin("stdlib-jdk8"))

    // Temporary workaround for kittinunf/fuel#757
    implementation("com.github.kittinunf.result:result:_")

    runtimeOnly("ch.qos.logback:logback-classic:_")

    testImplementation("io.kotest:kotest-runner-junit5:_")
    testImplementation("io.kotest:kotest-assertions-core-jvm:_")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:_")
    testImplementation("org.mockito:mockito-core:_")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = listOf("-XXLanguage:+InlineClasses", "-Xopt-in=kotlin.RequiresOptIn")
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

group = "org.danilopianini" // This must be configured for the generated pom.xml to work correctly

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
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

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "org.danilopianini.upgradle.UpGradle")
    }
}

tasks.withType<DokkaTask> {
    // Workaround for https://github.com/Kotlin/dokka/issues/294
    outputFormat = if (JavaVersion.current().isJava10Compatible) "html" else "javadoc"
    outputDirectory = "$buildDir/javadoc"
    tasks.withType<JavadocJar> {
        from(outputDirectory)
    }
}

val githubToken: String? by project
val ghActualToken = githubToken ?: System.getenv("GITHUB_TOKEN")
val jarTasks = tasks.withType<Jar>() + tasks.withType<JavadocJar>() + tasks.withType<SourcesJar>()
tasks.withType<GithubReleaseTask> {
    dependsOn(jarTasks)
}
if (ghActualToken != null) {
    githubRelease {
        token(ghActualToken)
        tagName(project.version.toString())
        owner.set("DanySK")
        prerelease { !project.version.toString().matches(Regex("""\d+(\.\d+)*""")) }
        releaseAssets(*jarTasks.map { it.archiveFile }.toTypedArray())
        body("## CHANGELOG\n${ changelog().call() }")
        allowUploadToExisting { true }
    }
}

if (System.getenv("CI") == true.toString()) {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

publishOnCentral {
    projectDescription.set(
        "A bot meant to pack updates and send pull requests." +
            "Much like dependabot, but way more homemade and focused on Gradle."
    )
    projectLongName.set("UpGradle")
    licenseName.set("GPL")
    licenseUrl.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
}

publishing {
    publications {
        withType<MavenPublication>() {
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
