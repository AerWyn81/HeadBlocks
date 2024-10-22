plugins {
    java
}

val mcVersion = providers.gradleProperty("mcVersion").get()

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(":spigot"))
    compileOnly(project(":paper"))

    implementation("org.spigotmc:spigot-api:${mcVersion}")
}