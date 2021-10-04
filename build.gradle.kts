import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.0"
}

version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    minimize()

    if (project.hasProperty("cd")) {
        // For ci/cd
        archiveFileName.set("HeadBlocks.jar")
    } else {
        archiveFileName.set("HeadBlocks-${archiveVersion.getOrElse("unknown")}.jar")
    }
    destinationDirectory.set(file(System.getenv("outputDir") ?: "$rootDir/build/"))
}

bukkit {
    name = "HeadBlocks"
    main = "fr.aerwyn81.headblocks.HeadBlocks"
    authors = listOf("AerWyn81")
    apiVersion = "1.16"
    description = "Attrapez les toutes!"
    version = rootProject.version.toString()
    website = "https://just2craft.fr"

    commands {
        register("headblocks") {
            description = "Commande du plugin"
            aliases = listOf("hb")
        }
    }

    permissions {
        register("headblocks.use") {
            description = "Allows players to interact with heads"
            default = BukkitPluginDescription.Permission.Default.NOT_OP
        }
        register("headblocks.admin") {
            description = "Allows access to /headblocks admin commands"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks.build {
    dependsOn("shadowJar")
}