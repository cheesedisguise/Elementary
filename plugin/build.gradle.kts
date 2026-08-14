plugins {
    java
}

group = "dev.elementary"
version = "0.1.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}
