plugins {
    id("java")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18-rc3-R0.1-SNAPSHOT")
    implementation(project(":common"))
}