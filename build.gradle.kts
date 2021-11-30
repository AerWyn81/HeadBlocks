plugins {
    id("java")
}

version = "1.3.0"

rootProject.allprojects {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public")
    }
}