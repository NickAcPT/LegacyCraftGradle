package io.github.nickacpt.legacycraftcraft.config

import io.github.nickacpt.legacycraftcraft.providers.*
import io.github.nickacpt.legacycraftcraft.tasks.ApplyMixinsTask
import org.gradle.api.artifacts.Configuration

open class LegacyCraftExtension {
    var version: ClientVersion = ClientVersion.ONE_FIVE_TWO

    internal lateinit var mappingsProvider: MappingsProvider
    internal lateinit var minecraftProvider: MinecraftProvider
    internal lateinit var minecraftLibraryProvider: MinecraftLibraryProvider
    internal lateinit var mixinProvider: MixinProvider
    internal lateinit var nativesProvider: MinecraftNativesProvider
    internal lateinit var launchProvider: MinecraftLaunchProvider
    internal lateinit var minecraftLibConfiguration: Configuration
    internal lateinit var launchWrapperConfiguration: Configuration
    internal lateinit var applyMixinsTask: ApplyMixinsTask
}