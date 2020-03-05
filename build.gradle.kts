import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:+")
    implementation("org.eclipse.jgit:org.eclipse.jgit:+")
    implementation("com.uchuhimo:konf:+")
    implementation("io.github.classgraph:classgraph:4.8.65")
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
