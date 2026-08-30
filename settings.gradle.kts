pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mappls (MapmyIndia) SDK artifacts
        maven { url = uri("https://maven.mappls.com/repository/mappls/") }
    }
}

rootProject.name = "PieceWork"
include(":app")
