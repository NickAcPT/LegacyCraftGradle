package io.github.nickacpt.legacycraftcraft.providers

import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftcraft.utils.LIBRARIES_BASE
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.maven


class MinecraftLibraryProvider(val project: Project) {

    fun provide() {
        project.legacyCraftExtension.minecraftLibConfiguration =  project.configurations.create("minecraftlib") {
            it.extendsFrom(project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME))
        }
        project.repositories.maven(LIBRARIES_BASE)

        val versionInfo: MinecraftVersionMeta = project.legacyCraftExtension.minecraftProvider.minecraftVersionMeta

        for (library in versionInfo.libraries) {
            if (library.isValidForOS && !library.hasNatives() && library.artifact != null) {
                project.dependencies.add(
                    "minecraftlib",
                    library.name
                )
            }
        }
    }
}