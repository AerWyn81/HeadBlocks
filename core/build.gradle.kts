import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

version = rootProject.version

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.5-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.2")
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.8")
    compileOnly("me.filoghost.holographicdisplays:holographicdisplays-api:3.0.4")
    compileOnly(files("../libs/CMILib1.4.7.16.jar"))
    compileOnly(files("../libs/CMI-9.7.3.2.jar"))
    implementation(files("../libs/holoeasy-core-3.4.1.jar"))
    //implementation("com.github.unldenis.holoeasy:holoeasy-core:3.4.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    implementation("redis.clients:jedis:5.1.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.tr7zw:item-nbt-api:2.13.1")
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_21.toString()
        targetCompatibility = JavaVersion.VERSION_21.toString()
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