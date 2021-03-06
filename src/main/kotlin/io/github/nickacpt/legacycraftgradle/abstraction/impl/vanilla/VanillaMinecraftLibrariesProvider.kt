package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.abstraction.GameAbstractionMinecraftLibrariesProvider
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftgradle.utils.LIBRARIES_BASE
import org.gradle.kotlin.dsl.maven

class VanillaMinecraftLibrariesProvider(val vanillaGameVersionImpl: VanillaGameVersionImpl) :
    GameAbstractionMinecraftLibrariesProvider {
    companion object {

        private const val mixinsVersion = "0.9.2+mixin.0.8.2"
        private const val asmVersion = "9.1"
        const val mixinDependency = "net.fabricmc:sponge-mixin:$mixinsVersion"
        const val asmDependency = "org.ow2.asm:asm:$asmVersion"
        const val asmUtilDependency = "org.ow2.asm:asm-util:$asmVersion"
    }

    override fun provideMinecraftLibraries(libraryConfigurationName: String) {
        val blacklistedDependencies = listOf("launchwrapper", "asm-all")
        val project = vanillaGameVersionImpl.project
        project.repositories.maven(LIBRARIES_BASE)

        val versionInfo: MinecraftVersionMeta = vanillaGameVersionImpl.minecraftProvider.minecraftVersionMeta

        for (library in versionInfo.libraries ?: emptyList()) {
            if (library.isValidForOS && !library.hasNatives() && library.artifact != null) {
                if (library.name != null && blacklistedDependencies.none { library.name.contains(it, true) })
                    project.dependencies.add(
                        libraryConfigurationName,
                        library.name
                    )
            }
        }

        // Provide updated ow2 asm
        project.dependencies.add(
            libraryConfigurationName,
            asmDependency
        )
        // Provide javax annotations
        project.dependencies.add(
            libraryConfigurationName,
            "javax.annotation:javax.annotation-api:1.3.2"
        )

        // Provide Mixins
        project.dependencies.add(
            libraryConfigurationName,
            mixinDependency
        )
    }


}