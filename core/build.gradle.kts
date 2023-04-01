plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
}

version = rootProject.version

repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.7.9")
    compileOnly("me.filoghost.holographicdisplays:holographicdisplays-api:3.0.0")
    implementation(files("../libs/hologram-lib-1.4.0-BETA.jar"))
    implementation("redis.clients:jedis:4.2.3")
    implementation("de.tr7zw:item-nbt-api:2.11.2")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.github.unldenis:Hologram-Lib:1.4.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.8")

    // Lamp
    implementation("com.github.Revxrsal.Lamp:common:3.1.5")
    implementation("com.github.Revxrsal.Lamp:bukkit:3.1.5")
    implementation("com.github.Revxrsal.Lamp:brigadier:3.1.5")

    // Adventure
    implementation("net.kyori:adventure-api:4.11.0")
    implementation("net.kyori:adventure-platform-bukkit:4.1.2")
    implementation("net.kyori:adventure-text-minimessage:4.11.0")
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
        options.encoding = "UTF-8"
    }

    jar {
        dependsOn("shadowJar")
    }

    shadowJar {
        relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.headblocks.bukkit.shaded.nbtapi")
        relocate("com.github.stefvanschie.inventoryframework", "fr.aerwyn81.headblocks.bukkit.shaded.inventoryframework")

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
    softDepend = listOf("PlaceholderAPI", "HeadDatabase", "ProtocolLib")
    version = rootProject.version.toString()
    website = "https://just2craft.fr"
}