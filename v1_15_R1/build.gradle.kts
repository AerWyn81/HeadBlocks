plugins {
    id("java")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.15.2-R0.1-SNAPSHOT")
    implementation(project(":common"))
}