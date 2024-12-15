plugins {
    id("java")
}

version = "2.6.14"

allprojects {
    repositories {
        mavenCentral()

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.fancyplugins.de/releases")
        maven("https://jitpack.io")
        maven("https://ci.codemc.io/job/Tr7zw/job/Item-NBT-API/")
    }
}