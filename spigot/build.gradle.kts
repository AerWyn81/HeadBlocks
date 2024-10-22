plugins {
    java
}

val mcVersion = providers.gradleProperty("mcVersion").get()

dependencies {
    compileOnly(project(":common"))

    compileOnly("org.spigotmc:spigot-api:${mcVersion}")
}