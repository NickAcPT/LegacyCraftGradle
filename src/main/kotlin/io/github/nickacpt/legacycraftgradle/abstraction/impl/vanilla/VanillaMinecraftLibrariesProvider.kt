package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.abstraction.GameAbstractionMinecraftLibrariesProvider
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftgradle.utils.LIBRARIES_BASE
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.maven

class VanillaMinecraftLibrariesProvider(val vanillaGameVersionImpl: VanillaGameVersionImpl) : GameAbstractionMinecraftLibrariesProvider {
    override fun provideMinecraftLibraries(libraryConfigurationName: String) {
        val project = vanillaGameVersionImpl.project
        project.repositories.maven(LIBRARIES_BASE)

        val versionInfo: MinecraftVersionMeta = vanillaGameVersionImpl.minecraftProvider.minecraftVersionMeta

        for (library in versionInfo.libraries ?: emptyList()) {
            if (library.isValidForOS && !library.hasNatives() && library.artifact != null) {
                if (library.name != null)
                    project.dependencies.add(
                        libraryConfigurationName,
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