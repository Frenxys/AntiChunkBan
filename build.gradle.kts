plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "com.enea"
version = "1.0.0"
description = "AntiBookBan - chunk ban protection: detects/removes oversized shulker boxes and limits books (25 pages, ASCII only)."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // We compile against the Paper 1.21 API: Paper maintains backward compatibility,
    // so the same jar works on servers from 1.21 to 1.26.2+ without recompiling.
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    // Adventure NBT is NOT provided by Paper at runtime: it gets shaded into the jar
    // (relocated) so the plugin works on all versions.
    implementation("net.kyori:adventure-nbt:4.17.0")
}

java {
    // Java 21 bytecode: this is the minimum requirement of Paper 1.21+.
    // The jar also runs on newer servers (Java 21+ / 25+).
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("AntiBookBan")
    archiveVersion.set("")
    archiveClassifier.set("")
    // Relocate adventure-nbt (and its examination dependency) to avoid
    // conflicts with any copies already present on the server.
    relocate("net.kyori.adventure.nbt", "com.enea.antibookban.libs.adventure.nbt")
    relocate("net.kyori.examination", "com.enea.antibookban.libs.examination")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}