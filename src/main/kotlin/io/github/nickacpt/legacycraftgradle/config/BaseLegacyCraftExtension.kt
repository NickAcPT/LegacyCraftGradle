package io.github.nickacpt.legacycraftgradle.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.github.nickacpt.legacycraftgradle.abstraction.GameVersionAbstraction
import io.github.nickacpt.legacycraftgradle.providers.*
import io.github.nickacpt.legacycraftgradle.tasks.ApplyMixinsTask
import org.gradle.api.artifacts.Configuration
import java.io.File

abstract class BaseLegacyCraftExtension {

    var runDeobfuscatedClient: Boolean = false

    var buildJarMod: Boolean = true

    var buildJarModAgent: Boolean = false

    var buildDeobfuscatedJar: Boolean = false

    var runMixinWithTweaker: Boolean = false

    var extraJarMods = mutableListOf<String>()

    internal lateinit var abstraction: GameVersionAbstraction<*>

    internal lateinit var mappingsProvider: MappingsProvider
    internal lateinit var minecraftProvider: MinecraftProvider
    internal lateinit var minecraftLibraryProvider: MinecraftLibraryProvider
    internal lateinit var mixinProvider: MixinProvider
    internal lateinit var nativesProvider: MinecraftNativesProvider
    internal lateinit var launchProvider: MinecraftLaunchProvider
    internal lateinit var minecraftLibConfiguration: Configuration
    internal lateinit var launchWrapperConfiguration: Configuration
    internal lateinit var applyMixinsTask: ApplyMixinsTask
    internal lateinit var assetsProvider: MinecraftAssetsProvider

    internal val mapper = jsonMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    
    internal lateinit var minecraftDependencyLocation: File
}