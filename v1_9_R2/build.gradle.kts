plugins {
    id("java")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.9.4-R0.1-SNAPSHOT")
    implementation(project(":common"))
}