include("HeadBlocks")
include("core")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        maven("https://jitpack.io")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}