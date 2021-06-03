package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.abstraction.GameVersionAbstraction
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

class VanillaGameVersionImpl : GameVersionAbstraction<VanillaLegacyCraftExtension> {

    override fun init() {
        project.extensions.create("legacycraft", VanillaLegacyCraftExtension::class)
    }

    override fun getExtension(): VanillaLegacyCraftExtension {
        return project.extensions.getByType(VanillaLegacyCraftExtension::class)
    }

    override fun getVersionName(): String {
        return project.getLegacyCraftExtension().version.launcherId
    }

    override fun getMappingUrls(): List<String> {
        return listOf(
            "https://raw.githubusercontent.com/NickAcPT/LegacyCraftMappings/main/${project.getLegacyCraftExtension().version}/client.tinyv2?v=${System.currentTimeMillis()}"
        )
    }

    override lateinit var project: Project
    override val minecraftProvider: VanillaMinecraftProvider = VanillaMinecraftProvider(this)
    override val librariesProvider: VanillaMinecraftLibrariesProvider = VanillaMinecraftLibrariesProvider(this)
    override val nativesProvider: VanillaMinecraftNativesProvider = VanillaMinecraftNativesProvider(this)
    override val runProvider: VanillaMinecraftRunProvider = VanillaMinecraftRunProvider(this)
}