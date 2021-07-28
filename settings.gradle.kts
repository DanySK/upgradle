import org.danilopianini.VersionAliases.justAdditionalAliases

plugins {
    id("de.fayard.refreshVersions") version "0.10.1"
    id("com.gradle.enterprise") version "3.6.3"
}

refreshVersions {
    extraArtifactVersionKeyRules = justAdditionalAliases
}

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("org.danilopianini:refreshversions-aliases:+")
    }
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

rootProject.name = "upgradle"
