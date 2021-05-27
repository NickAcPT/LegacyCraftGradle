package io.github.nickacpt.legacycraftcraft.config

import io.github.nickacpt.legacycraftcraft.providers.*

open class LegacyCraftExtension {
    var version: ClientVersion = ClientVersion.ONE_FIVE_TWO

    internal lateinit var mappingsProvider: MappingsProvider
    internal lateinit var minecraftProvider: MinecraftProvider
    internal lateinit var minecraftLibraryProvider: MinecraftLibraryProvider
    internal lateinit var mixinProvider: MixinProvider
    internal lateinit var nativesProvider: MinecraftNativesProvider
    internal lateinit var launchProvider: MinecraftLaunchProvider
}