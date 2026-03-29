import io.papermc.hangarpublishplugin.model.Platforms
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    jacoco
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.hangar.publish)
}

version = "3.0.0"

val coverageExclusions = listOf(
    // Third-party shaded libraries
    "**/utils/message/color/IridiumColorAPI.java",
    "**/utils/message/DefaultFont.java",

    // NMS-dependent (NBTAPI requires Minecraft server internals)
    "**/utils/bukkit/HeadAdapterNbtApi.java",
    "**/databases/types/MySQL.java",

    // Plugin main class & Bukkit infrastructure
    "**/HeadBlocks.java",
    "**/utils/bukkit/BukkitSchedulerAdapter.java",
    "**/utils/bukkit/HeadBlocksPluginProvider.java",
    "**/utils/bukkit/BukkitCommandDispatcher.java",
    "**/utils/bukkit/FireworkUtils.java",
    "**/utils/bukkit/ParticlesUtils.java",
    "**/utils/bukkit/ItemBuilder.java",

    // GUI (Bukkit inventory API)
    "**/services/gui/**",
    "**/services/GuiService.java",

    // Holograms (external plugin dependency)
    "**/services/HologramService.java",
    "**/holograms/types/**",
    "**/holograms/InternalHologram.java",

    // Hooks (external plugin dependencies)
    "**/hooks/HeadHidingPacketListener.java",
    "**/hooks/PacketEventsHook.java",
    "**/hooks/PacketEventsHookImpl.java",
    "**/hooks/HeadDatabaseHook.java",

    // Events (Bukkit-bound, not unit-testable)
    "**/events/OnHeadDatabaseLoaded.java",
    "**/events/OnPlayerClickInventoryEvent.java",
    "**/events/OnPlayerChatEvent.java",
    "**/events/OthersEvent.java",

    // Runnables (Bukkit scheduler-bound)
    "**/runnables/GlobalTask.java",
    "**/runnables/TimedRunTask.java",
    "**/runnables/CompletableBukkitFuture.java",

    // Misc untestable
    "**/utils/internal/DebugLog.java",
    "**/commands/list/Debug.java"
)

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.headdatabase.api)
    compileOnly(libs.packetevents)

    implementation(libs.jedis)
    implementation(libs.hikaricp)
    implementation(libs.slf4j.jdk14)
    implementation(libs.gson)
    implementation(libs.commons.lang3)
    implementation(libs.nbt.api)
    implementation(libs.holoeasy)
    implementation(libs.bstats)
    implementation(libs.xseries)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.placeholderapi)
    testImplementation(libs.packetevents)
}

tasks {
    runServer {
        minecraftVersion("1.21.11")

        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("terminal.ansi", true)
    }

    test {
        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            csv.required.set(true)
            xml.required.set(true)
        }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(coverageExclusions.map { pattern ->
                        pattern.replace(".java", "*")
                    })
                }
            })
        )
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
        mergeServiceFiles()

        relocate("com.google.gson", "fr.aerwyn81.libs.gson")
        relocate("com.jetbrains.annotations", "fr.headblocks.libs.gson")
        relocate("redis.clients.jedis", "fr.aerwyn81.libs.jedis")
        relocate("org.apache.commons", "fr.aerwyn81.libs.commons-lang3")
        relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.libs.nbtapi")
        relocate("org.holoeasy", "fr.aerwyn81.libs.holoEasy")
        relocate("org.json", "fr.aerwyn81.libs.json")
        relocate("org.slf4j", "fr.aerwyn81.libs.slf4j")
        relocate("com.zaxxer.hikari", "fr.aerwyn81.libs.hikari")
        relocate("org.bstats", "fr.aerwyn81.libs.bstats")
        relocate("com.cryptomorin.xseries", "fr.aerwyn81.libs.xseries")

        // Exclude unused XSeries modules (only XSound is used)
        exclude("com/cryptomorin/xseries/XAttribute*")
        exclude("com/cryptomorin/xseries/XBiome*")
        exclude("com/cryptomorin/xseries/XBlock*")
        exclude("com/cryptomorin/xseries/XEnchantment*")
        exclude("com/cryptomorin/xseries/XEntityType*")
        exclude("com/cryptomorin/xseries/XItemFlag*")
        exclude("com/cryptomorin/xseries/XMaterial*")
        exclude("com/cryptomorin/xseries/XParticle*")
        exclude("com/cryptomorin/xseries/XPotion*")
        exclude("com/cryptomorin/xseries/XTag*")
        exclude("com/cryptomorin/xseries/XWorldBorder*")
        exclude("com/cryptomorin/xseries/particles/**")

        if (project.hasProperty("cd"))
            archiveFileName.set("HeadBlocks.jar")
        else
            archiveFileName.set("HeadBlocks-${archiveVersion.getOrElse("unknown")}.jar")

        destinationDirectory.set(file(System.getenv("outputDir") ?: "$rootDir/build/"))

        minimize {
            exclude(dependency("com.zaxxer:HikariCP:.*"))
            exclude(dependency("org.slf4j:slf4j-jdk14:.*"))
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "AerWyn81_HeadBlocks")
        property("sonar.organization", "aerwyn81")
        property("sonar.coverage.exclusions", coverageExclusions.joinToString(","))
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

hangarPublish {
    publications.register("plugin") {
        version = project.version as String
        id = "HeadBlocks"
        channel = "Release"
        changelog = System.getenv("HANGAR_CHANGELOG") ?: ""
        apiKey = System.getenv("HANGAR_API_TOKEN") ?: ""

        platforms {
            register(Platforms.PAPER) {
                jar = tasks.shadowJar.flatMap { it.archiveFile }
                platformVersions = listOf(
                    "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
                    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
                    "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"
                )
                dependencies {
                    hangar("PlaceholderAPI") {
                        required = false
                    }
                    hangar("HeadDatabase") {
                        required = false
                    }
                    hangar("packetevents") {
                        required = false
                    }
                }
            }
        }
    }
}
