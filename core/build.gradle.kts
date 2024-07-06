import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
}

version = rootProject.version

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.2")
    compileOnly("me.filoghost.holographicdisplays:holographicdisplays-api:3.0.0")
    compileOnly(files("../libs/CMILib1.4.3.4.jar"))
    compileOnly(files("../libs/CMI-9.6.8.5.jar"))
    implementation("com.github.AerWyn81:Hologram-Lib:2.6.0-fork")
    implementation("redis.clients:jedis:4.4.3")
    implementation("de.tr7zw:item-nbt-api:2.12.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
        options.encoding = "UTF-8"
    }

    jar {
        dependsOn("shadowJar")
    }

    shadowJar {
        relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.headblocks.bukkit.shaded.nbtapi")

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
    softDepend = listOf("PlaceholderAPI", "HeadDatabase", "ProtocolLib", "DecentHolograms", "CMI")
    version = rootProject.version.toString()
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