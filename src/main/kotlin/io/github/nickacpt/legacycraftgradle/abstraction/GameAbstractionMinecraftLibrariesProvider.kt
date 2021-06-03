package io.github.nickacpt.legacycraftgradle.abstraction

interface GameAbstractionMinecraftLibrariesProvider {
    fun provideMinecraftLibraries(libraryConfigurationName: String)
}