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
    id("com.gradle.enterprise") version "3.6.3"
    id("de.fayard.refreshVersions") version "0.10.1"
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
