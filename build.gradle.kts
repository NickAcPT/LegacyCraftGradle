plugins {
    kotlin("jvm") version "1.5.0"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "io.github.nickacpt"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    // I/O
    implementation ("commons-io:commons-io:2.8.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("org.zeroturnaround:zt-zip:1.14")

    // Jar Remapping
    implementation("net.fabricmc:tiny-remapper:0.3.2")
    implementation("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")

    implementation("io.github.nickacpt:MixinOfflineApplierTool:1.0-SNAPSHOT") {
        exclude(module = "clikt")
    }
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "16"
        sourceCompatibility = "16"
        targetCompatibility = "16"
    }
}

gradlePlugin {
    plugins {
        create("LegacyCraftGradle") {
            id = "io.github.nickacpt.legacycraftgradle"
            implementationClass = "io.github.nickacpt.legacycraftcraft.LegacyCraftPlugin"
        }
    }
}