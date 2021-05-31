import org.danilopianini.VersionAliases.justAdditionalAliases
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.danilopianini:refreshversions-aliases:+")
    }
}

plugins {
    id("com.gradle.enterprise") version "3.2"
    id("de.fayard.refreshVersions") version "0.10.0"
}

refreshVersions {
    extraArtifactVersionKeyRules = justAdditionalAliases
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

rootProject.name = "upgradle"
