package io.github.nickacpt.legacycraftcraft.providers

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.maven

class MixinProvider(val project: Project) {

    val mixinVersion = "0.9.2+mixin.0.8.2"
    fun provide() {
        project.repositories.maven("https://maven.fabricmc.net/")

        project.dependencies.add(
            JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
            "net.fabricmc:sponge-mixin:$mixinVersion"
        )
    }
}