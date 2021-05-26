package io.github.nickacpt.legacycraftcraft.config

import io.github.nickacpt.legacycraftcraft.providers.MappingsProvider
import io.github.nickacpt.legacycraftcraft.providers.MinecraftLibraryProvider
import io.github.nickacpt.legacycraftcraft.providers.MinecraftProvider
import io.github.nickacpt.legacycraftcraft.providers.MixinProvider

open class LegacyCraftExtension {
    var version: ClientVersion = ClientVersion.ONE_FIVE_TWO

    internal lateinit var mappingsProvider: MappingsProvider
    internal lateinit var minecraftProvider: MinecraftProvider
    internal lateinit var minecraftLibraryProvider: MinecraftLibraryProvider
    internal lateinit var mixinProvider: MixinProvider
}