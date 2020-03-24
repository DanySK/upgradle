import de.fayard.dependencies.bootstrapRefreshVersionsAndDependencies

rootProject.name = "upgradle"

buildscript {
    repositories { gradlePluginPortal() }
    dependencies.classpath("de.fayard:dependencies:0.5.7")
}

bootstrapRefreshVersionsAndDependencies()
