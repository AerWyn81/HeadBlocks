import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
}

version = "2.8.2"

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.headdatabase.api)
    compileOnly(libs.packetevents)

    implementation(libs.jedis)
    implementation(libs.hikaricp)
    implementation(libs.gson)
    implementation(libs.commons.lang3)
    implementation(libs.nbt.api)
    implementation(libs.holoeasy)
    implementation("com.tcoded:FoliaLib:0.5.1")
}

tasks {
    runServer {
        minecraftVersion("1.21.8")

        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("terminal.ansi", true)
    }

    compileJava {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
        options.encoding = "UTF-8"
    }

    jar {
        dependsOn("shadowJar")
    }

    shadowJar {
        relocate("com.google.gson", "fr.aerwyn81.libs.gson")
        relocate("com.jetbrains.annotations", "fr.headblocks.libs.gson")
        relocate("redis.clients.jedis", "fr.aerwyn81.libs.jedis")
        relocate("org.apache.commons", "fr.aerwyn81.libs.commons-lang3")
        relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.libs.nbtapi")
        relocate("org.holoeasy", "fr.aerwyn81.libs.holoEasy")
        relocate("org.json", "fr.aerwyn81.libs.json")
        relocate("org.slf4j", "fr.aerwyn81.libs.slf4j")
        relocate("com.tcoded.folialib", "fr.aerwyn81.headblocks.lib.folialib")

        if (project.hasProperty("cd"))
            archiveFileName.set("HeadBlocks.jar")
        else
            archiveFileName.set("HeadBlocks-${archiveVersion.getOrElse("unknown")}.jar")

        destinationDirectory.set(file(System.getenv("outputDir") ?: "$rootDir/build/"))

        minimize()
    }
}

bukkit {
    name = "HeadBlocks"
    main = "fr.aerwyn81.headblocks.HeadBlocks"
    authors = listOf("AerWyn81")
    apiVersion = "1.13"
    description = "Challenge your players to find all the heads and earn rewards"
    softDepend = listOf("PlaceholderAPI", "HeadDatabase", "packetevents")
    version = project.version.toString()
    website = "https://just2craft.fr"

    commands {
        register("headblocks") {
            description = "Plugin command"
            aliases = listOf("hb")
        }
    }

    permissions {
        register("headblocks.use") {
            description = "Allows players to interact with heads and see their progress"
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("headblocks.commands.top") {
            description = "Allows players to see leaderboard"
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("headblocks.commands.progress") {
            description = "Allows players to see his or player score"
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("headblocks.admin") {
            description = "Allows access to /headblocks admin commands"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}
