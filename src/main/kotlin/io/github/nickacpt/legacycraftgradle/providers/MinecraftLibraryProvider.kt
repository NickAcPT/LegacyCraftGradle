package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin


class MinecraftLibraryProvider(val project: Project) {

    fun provide() {

        val libraryConfigurationName = "minecraftlib"
        project.legacyCraftExtension.minecraftLibConfiguration = project.configurations.create(libraryConfigurationName) {
            project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(it)
        }

        project.legacyCraftExtension.abstraction.librariesProvider.provideMinecraftLibraries(libraryConfigurationName)
    }
}