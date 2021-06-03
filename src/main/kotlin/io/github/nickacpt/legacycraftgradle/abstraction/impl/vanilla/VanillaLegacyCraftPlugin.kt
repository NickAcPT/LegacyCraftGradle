package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.BaseLegacyCraftPlugin

class VanillaLegacyCraftPlugin : BaseLegacyCraftPlugin() {
    init {
        abstraction = VanillaGameVersionImpl()
    }
}