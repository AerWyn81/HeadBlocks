import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.0"
}

version = rootProject.version

repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.10.10")
    implementation("redis.clients:jedis:3.6.3")
    implementation("de.tr7zw:item-nbt-api:2.9.0-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.javatuples:javatuples:1.2")
    implementation("org.yaml:snakeyaml:1.29")
    implementation(project(":common"))
    implementation(project(":v1_8_R1"))
    implementation(project(":v1_9_R2"))
    implementation(project(":v1_10_R1"))
    implementation(project(":v1_11_R1"))
    implementation(project(":v1_12_R1"))
    implementation(project(":v1_13_R2"))
    implementation(project(":v1_14_R1"))
    implementation(project(":v1_15_R1"))
    implementation(project(":v1_16_R3"))
    implementation(project(":v1_17_R1"))
    implementation(project(":v1_18_R1"))
}

tasks {
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.encoding = "UTF-8"
    }

    jar {
        dependsOn("shadowJar")
    }

    shadowJar {
        relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.headblocks.bukkit.shaded.nbtapi")
        relocate("org.yaml.snakeyaml", "fr.aerwyn81.headblocks.bukkit.shaded.snakeyaml")

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
    softDepend = listOf("PlaceholderAPI", "TitleAPI")
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
            default = BukkitPluginDescription.Permission.Default.NOT_OP
        }
        register("headblocks.admin") {
            description = "Allows access to /headblocks admin commands"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}