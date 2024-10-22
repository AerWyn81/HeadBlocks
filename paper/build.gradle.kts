plugins {
    java
}

val mcVersion = providers.gradleProperty("mcVersion").get()

dependencies {
    compileOnly(project(":common"))

    compileOnly("io.papermc.paper:paper-api:${mcVersion}")
}