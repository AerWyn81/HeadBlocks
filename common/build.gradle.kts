plugins {
    java
}

val mcVersion = providers.gradleProperty("mcVersion").get()

dependencies {
    implementation("org.spigotmc:spigot-api:${mcVersion}")
}