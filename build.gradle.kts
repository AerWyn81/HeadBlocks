import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.hangarpublishplugin.model.Platforms
import net.minecrell.pluginyml.GeneratePluginDescription
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import xyz.jpenilla.runpaper.RunPaperExtension
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    jacoco
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.hangar.publish)
}

version = "3.1.0"

val coverageExclusions = listOf(
    // Third-party shaded libraries
    "**/utils/message/color/IridiumColorAPI.java",
    "**/utils/message/DefaultFont.java",

    // NMS-dependent (NBTAPI requires Minecraft server internals)
    "**/utils/bukkit/HeadAdapterNbtApi.java",
    "**/databases/types/MySQL.java",

    // Plugin main class & Bukkit infrastructure
    "**/HeadBlocks.java",
    "**/utils/scheduler/BukkitSchedulerAdapter.java",
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
    "**/hooks/HeadDBHook.java",

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
    maven("https://maven.enginehub.org/repo/")
}

// `main` compiles against spigot-api on purpose: it prevents Paper-only API from leaking into common code.
// Classpaths are extended rather than declared as `implementation(main.output)` dependencies: only the
// former is translated into module dependencies by IntelliJ's Gradle import.
val spigot = sourceSets.create("spigot") {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

val paper = sourceSets.create("paper") {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.headdatabase.api)
    compileOnly(libs.headdb.api)
    compileOnly(libs.packetevents)
    compileOnly(libs.worldguard)

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

    spigot.compileOnlyConfigurationName(libs.spigot.api)
    paper.compileOnlyConfigurationName(libs.paper.api)
}

fun BukkitPluginDescription.describeHeadBlocks() {
    name = "HeadBlocks"
    main = "fr.aerwyn81.headblocks.HeadBlocks"
    authors = listOf("AerWyn81")
    apiVersion = "1.13"
    description = "Challenge your players to find all the heads and earn rewards"
    softDepend = listOf("PlaceholderAPI", "HeadDatabase", "HeadDB", "packetevents", "WorldGuard")
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
        register("headblocks.zone.bypass") {
            description = "Allows bypassing hunt zone confinement"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

// The extension feeds the Spigot descriptor only. The Paper flavour ships a paper-plugin.yml
// instead, which is a different format with its own description type and generation task.
bukkit {
    describeHeadBlocks()
}

// paper-plugin.yml deliberately has no commands section: the command is registered at runtime by
// PaperPlatform. Dependencies move from softDepend to serverDependencies because Paper plugins get
// an isolated classloader — joinClasspath is what keeps the optional hooks able to see their API.
val paperPluginDescription = PaperPluginDescription(project).apply {
    name = "HeadBlocks"
    main = "fr.aerwyn81.headblocks.HeadBlocks"
    authors = listOf("AerWyn81")
    // Paper plugins require at least 1.19; the runtime check in HeadBlocks already demands 1.20+.
    apiVersion = "1.20"
    description = "Challenge your players to find all the heads and earn rewards"
    version = project.version.toString()
    website = "https://just2craft.fr"
    foliaSupported = true

    serverDependencies {
        for (optional in listOf("PlaceholderAPI", "HeadDatabase", "HeadDB", "packetevents", "WorldGuard")) {
            register(optional) {
                required = false
                joinClasspath = true
                load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            }
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
        register("headblocks.zone.bypass") {
            description = "Allows bypassing hunt zone confinement"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

val generatePaperPluginDescription = tasks.register<GeneratePluginDescription>("generatePaperPluginDescription") {
    fileName.set("paper-plugin.yml")
    librariesJsonFileName.set("paper-libraries.json")
    pluginDescription.set(paperPluginDescription)
    outputDirectory.set(layout.buildDirectory.dir("generated/paper-plugin-yml"))
}

val mcVersion = "26.2"

// Minecraft 26.1+ refuses to boot below Java 25, whatever JDK happens to run Gradle.
val serverJavaLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

// Opens JDWP for terminal runs, where IntelliJ cannot inject a debugger. Must stay opt-in:
// setting debugOptions unconditionally finalizes the property and breaks the IDE's Debug button.
fun JavaExec.enableDebugPortIfRequested(port: Int) {
    if (project.hasProperty("debugServer")) {
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$port")
    }
}

fun ShadowJar.applySharedShadowConfig() {
    mergeServiceFiles()

    relocate("com.google.gson", "fr.aerwyn81.libs.gson")
    relocate("org.jetbrains.annotations", "fr.aerwyn81.libs.annotations")
    relocate("redis.clients.jedis", "fr.aerwyn81.libs.jedis")
    relocate("org.apache.commons", "fr.aerwyn81.libs.commons-lang3")
    relocate("de.tr7zw.changeme.nbtapi", "fr.aerwyn81.libs.nbtapi")
    relocate("org.holoeasy", "fr.aerwyn81.libs.holoEasy")
    relocate("org.json", "fr.aerwyn81.libs.json")
    relocate("org.slf4j", "fr.aerwyn81.libs.slf4j")
    relocate("com.zaxxer.hikari", "fr.aerwyn81.libs.hikari")
    relocate("org.bstats", "fr.aerwyn81.libs.bstats")
    relocate("com.cryptomorin.xseries", "fr.aerwyn81.libs.xseries")

    // Exclude unused XSeries modules (only XSound and XParticle are used)
    exclude("com/cryptomorin/xseries/XAttribute*")
    exclude("com/cryptomorin/xseries/XBiome*")
    exclude("com/cryptomorin/xseries/XBlock*")
    exclude("com/cryptomorin/xseries/XEnchantment*")
    exclude("com/cryptomorin/xseries/XEntityType*")
    exclude("com/cryptomorin/xseries/XItemFlag*")
    exclude("com/cryptomorin/xseries/XMaterial*")
    exclude("com/cryptomorin/xseries/XPotion*")
    exclude("com/cryptomorin/xseries/XTag*")
    exclude("com/cryptomorin/xseries/XWorldBorder*")

    destinationDirectory.set(file(System.getenv("outputDir") ?: "$rootDir/build/"))

    minimize {
        exclude(dependency("com.zaxxer:HikariCP:.*"))
        exclude(dependency("org.slf4j:slf4j-jdk14:.*"))
        // Reached only from MetricsBase method bodies; minimize has already dropped its inner classes once.
        exclude(dependency("org.bstats:.*:.*"))
    }
}

fun registerPlatformJar(taskName: String, platform: SourceSet, classifier: String, pluginDescription: Any) =
    tasks.register<ShadowJar>(taskName) {
        group = "shadow"
        description = "Builds the ${platform.name} flavoured plugin jar."

        from(sourceSets.main.get().output) { exclude("plugin.yml") }
        from(platform.output)
        from(pluginDescription)
        configurations.set(setOf(project.configurations.getByName("runtimeClasspath")))
        sourceSetsClassesDirs.from(sourceSets.main.get().output.classesDirs, platform.output.classesDirs)

        archiveClassifier.set(classifier)
        applySharedShadowConfig()

        val suffix = if (classifier.isEmpty()) "" else "-$classifier"
        if (project.hasProperty("cd")) {
            archiveFileName.set("HeadBlocks$suffix.jar")
        } else {
            archiveFileName.set("HeadBlocks-${project.version}$suffix.jar")
        }
    }

val paperJar = registerPlatformJar("paperJar", paper, "", generatePaperPluginDescription)
val spigotJar = registerPlatformJar("spigotJar", spigot, "spigot", tasks.named("generateBukkitPluginDescription"))

// Tests for one platform only. MockBukkit is deliberately absent: it drags in paper-api, which
// would put Paper on the spigotTest classpath and defeat the isolation.
fun registerPlatformTest(platformSet: SourceSet, platformApi: Provider<*>): TaskProvider<Test> {
    val testSet = sourceSets.create("${platformSet.name}Test") {
        compileClasspath += sourceSets["main"].output + platformSet.output
        runtimeClasspath += sourceSets["main"].output + platformSet.output
    }

    dependencies {
        testSet.implementationConfigurationName(platformApi)
        testSet.implementationConfigurationName(platform(libs.junit.bom))
        testSet.implementationConfigurationName(libs.junit.jupiter)
        testSet.implementationConfigurationName(libs.mockito.core)
        testSet.implementationConfigurationName(libs.mockito.junit)
        testSet.implementationConfigurationName(libs.assertj)
        testSet.runtimeOnlyConfigurationName(libs.junit.platform.launcher)
    }

    return tasks.register<Test>(testSet.name) {
        group = "verification"
        description = "Runs the ${platformSet.name} specific tests."

        testClassesDirs = testSet.output.classesDirs
        classpath = testSet.runtimeClasspath

        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}

val paperTest = registerPlatformTest(paper, libs.paper.api)
val spigotTest = registerPlatformTest(spigot, libs.spigot.api)

tasks {
    runServer {
        minecraftVersion(mcVersion)
        javaLauncher.set(serverJavaLauncher)
        // setFrom, not pluginJars(): the plugin appends an auto-detected shadowJar, which is disabled here.
        pluginJars.setFrom(paperJar.flatMap { it.archiveFile })

        args("--port", "25565")
        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("terminal.ansi", true)

        enableDebugPortIfRequested(5005)
    }

    // Server jar is not downloadable: build it once with BuildTools (see README).
    register<RunServer>("runSpigotServer") {
        minecraftVersion(mcVersion)
        javaLauncher.set(serverJavaLauncher)
        serverJar(file("run-spigot/spigot.jar"))
        runDirectory.set(file("run-spigot"))
        pluginJars.setFrom(spigotJar.flatMap { it.archiveFile })

        // `-add-plugin` is a Paper-only CLI flag; Spigot needs the jar copied into plugins/.
        legacyPluginLoading()

        args("--port", "25566")
        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("terminal.ansi", true)

        enableDebugPortIfRequested(5006)
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
        enabled = false
        dependsOn(paperJar, spigotJar)
    }

    check {
        dependsOn(paperTest, spigotTest)
    }

    // Superseded by paperJar / spigotJar.
    shadowJar {
        enabled = false
    }
}

// Folia is the whole point of the paper flavour, so it needs its own dev server. INHERIT_NONE
// because runServer's config is not reusable here: the plugin jar has to be set explicitly (the
// auto-detected shadowJar is disabled) and the run directory must not be shared with Paper.
runPaper {
    folia {
        pluginsMode.set(RunPaperExtension.Folia.PluginsMode.INHERIT_NONE)

        registerTask {
            minecraftVersion(mcVersion)
            javaLauncher.set(serverJavaLauncher)
            pluginJars.setFrom(paperJar.flatMap { it.archiveFile })
            runDirectory.set(file("run-folia"))

            args("--port", "25567")
            systemProperty("com.mojang.eula.agree", "true")
            systemProperty("terminal.ansi", true)

            // A heap exhaustion here dies without reaching the server log, so leave the evidence on disk.
            jvmArgs(
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-XX:HeapDumpPath=${file("run-folia").absolutePath}",
                "-Xlog:gc*:file=${file("run-folia/gc.log").absolutePath}:time,uptime:filecount=3,filesize=10M"
            )

            enableDebugPortIfRequested(5007)
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

hangarPublish {
    publications.register("plugin") {
        version = project.version as String
        id = "HeadBlocks"
        channel = "Release"
        changelog = System.getenv("HANGAR_CHANGELOG") ?: ""
        apiKey = System.getenv("HANGAR_API_TOKEN") ?: ""

        platforms {
            register(Platforms.PAPER) {
                jar = paperJar.flatMap { it.archiveFile }
                platformVersions = listOf(
                    "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
                    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
                    "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
                    "26.1", "26.1.1", "26.1.2", "26.2"
                )
                dependencies {
                    hangar("PlaceholderAPI") {
                        required = false
                    }
                    hangar("HeadDatabase") {
                        required = false
                    }
                    hangar("HeadDB") {
                        required = false
                    }
                    url("packetevents", "https://modrinth.com/plugin/packetevents") {
                        required = false
                    }
                }
            }
        }
    }
}
