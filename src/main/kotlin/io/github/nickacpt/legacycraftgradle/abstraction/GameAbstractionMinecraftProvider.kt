package io.github.nickacpt.legacycraftgradle.abstraction

import java.io.File

interface GameAbstractionMinecraftProvider {
    fun provideMinecraftJar(): File
}