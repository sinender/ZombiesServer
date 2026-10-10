import jdk.jfr.internal.JVM.exclude

plugins {
    kotlin("jvm")
    application
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

group = "net.sinender.zombies"
version = "1.0"

application.mainClass = "net.sinender.zombies.Main"
kotlin.jvmToolchain(25)

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://central.sonatype.com/repository/maven-snapshots")
    maven("https://repo.redstone.llc/releases")
    maven("https://reposilite.atlasengine.ca/public")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.fastutil)
    implementation(libs.schem)
    implementation(libs.pathfinding) {
        exclude(group = "net.minestom")
    }

    implementation(libs.tinylog.impl)
    implementation(libs.tinylog.slf4j)
}
