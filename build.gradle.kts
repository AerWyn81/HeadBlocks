plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

allprojects {
    group = "fr.aerwyn81"
    version = "3.0.0"

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

// Tâche pour générer le JAR Spigot
tasks.register<Jar>("spigotJar") {
    archiveClassifier.set("spigot")

    from(project(":common").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":spigot").sourceSets["main"].output)

    @Suppress("UnstableApiUsage")
    manifest {
        attributes["Main-Class"] = "fr.aerwyn81.HeadBlocks"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Jar>("paperJar") {
    archiveClassifier.set("paper")

    from(project(":common").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":paper").sourceSets["main"].output)

    @Suppress("UnstableApiUsage")
    manifest {
        attributes["Main-Class"] = "fr.aerwyn81.HeadBlocks"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register("buildAll") {
    dependsOn("spigotJar", "paperJar")
}