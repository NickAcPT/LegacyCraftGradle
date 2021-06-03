package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.config.BaseLegacyCraftExtension
import io.github.nickacpt.legacycraftgradle.config.ClientVersion

open class VanillaLegacyCraftExtension : BaseLegacyCraftExtension() {

    var version: ClientVersion = ClientVersion.ONE_FIVE_TWO

}