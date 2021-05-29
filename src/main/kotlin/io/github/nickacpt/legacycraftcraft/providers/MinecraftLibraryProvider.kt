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
            project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(it)
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

        // Provide updated ow2 asm
        val asmVersion = "9.1"
        project.dependencies.add(
            JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
            "org.ow2.asm:asm:$asmVersion"
        )
    }
}