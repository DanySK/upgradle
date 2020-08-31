import de.fayard.refreshVersions.bootstrapRefreshVersions
import org.danilopianini.VersionAliases.justAdditionalAliases
buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("de.fayard.refreshVersions:refreshVersions:0.9.5")
        classpath("org.danilopianini:refreshversions-aliases:+")
    }
}
bootstrapRefreshVersions(justAdditionalAliases)

plugins {
    id("com.gradle.enterprise") version "3.2"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

rootProject.name = "upgradle"
