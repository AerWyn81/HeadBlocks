import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

version = rootProject.version

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.2")
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.8")
    compileOnly("de.oliver:FancyHolograms:2.3.1")
    compileOnly(files("../libs/CMILib1.4.7.16.jar"))
    compileOnly(files("../libs/CMI-9.7.3.2.jar"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    implementation("redis.clients:jedis:5.1.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.tr7zw:item-nbt-api:2.15.1")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.3")
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
        relocate("kotlin", "fr.aerwyn81.libs.kotlin")
        relocate("com.google.gson", "fr.aerwyn81.libs.gson")
        relocate("com.jetbrains.annotations", "fr.headblocks.libs.gson")
        relocate("redis.clients.jedis", "fr.aerwyn81.libs.jedis")
        relocate("com.zaxxer", "fr.aerwyn81.libs.hikariCP")
        relocate("org.apache.commons", "fr.aerwyn81.libs.commons-lang3")
        relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.libs.nbtapi")
        relocate("org.lushplugins", "fr.aerwyn81.libs.chatColorHandler")
        relocate("org.holoeasy", "fr.aerwyn81.libs.holoEasy")
        relocate("org.jetbrains.annotations", "fr.aerwyn81.libs.jetbrains-annotations")
        relocate("org.json", "fr.aerwyn81.libs.json")
        relocate("org.slf4j", "fr.aerwyn81.libs.slf4j")
        relocate("net.kyori", "fr.aerwyn81.libs.adventure-api")

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
    softDepend = listOf("PlaceholderAPI", "HeadDatabase", "DecentHolograms", "CMI", "FancyHolograms")
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