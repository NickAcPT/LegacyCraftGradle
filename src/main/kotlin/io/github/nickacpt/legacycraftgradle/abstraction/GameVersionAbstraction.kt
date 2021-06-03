package io.github.nickacpt.legacycraftgradle.abstraction

import io.github.nickacpt.legacycraftgradle.config.BaseLegacyCraftExtension
import org.gradle.api.Project

interface GameVersionAbstraction<E: BaseLegacyCraftExtension> {

    var project: Project

    fun init()

    fun getExtension(): E

    fun getVersionName(): String

    @JvmSynthetic
    fun Project.getLegacyCraftExtension(): E {
        return getExtension()
    }

    fun getMappingUrls(): List<String>

    val minecraftProvider: GameAbstractionMinecraftProvider
    val librariesProvider: GameAbstractionMinecraftLibrariesProvider
    val nativesProvider: GameAbstractionMinecraftNativesProvider
    val runProvider: GameAbstractionMinecraftRunProvider?
}

