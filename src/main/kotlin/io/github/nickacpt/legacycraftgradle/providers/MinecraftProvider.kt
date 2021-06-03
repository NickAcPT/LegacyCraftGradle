package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.BaseLegacyCraftPlugin
import io.github.nickacpt.legacycraftgradle.config.BaseLegacyCraftExtension
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.utils.remapJar
import org.gradle.api.Project
import java.io.File


class MinecraftProvider(val project: Project) {

    val minecraftMappedJar by lazy { project.getCacheFile("minecraft-mapped.jar") }

    fun provide() {
        val craftExtension = project.legacyCraftExtension
        val outputFile = getFinalMinecraftJar()

        project.provideDependency("net.minecraft", "minecraft", craftExtension.abstraction.getVersionName(), outputFile)
    }

    private fun getFinalMinecraftJar(): File {
        val craftExtension = project.legacyCraftExtension

        val minecraftJar = computeMinecraftJar(craftExtension)

        if (!BaseLegacyCraftPlugin.refreshDeps && minecraftMappedJar.exists()) {
            return minecraftMappedJar
        }

        val mappings = craftExtension.mappingsProvider.getMappingsForVersion()
        val outputFile = minecraftMappedJar

        println(":mapping - Remapping Minecraft ${project.legacyCraftExtension.abstraction.getVersionName()}")
        remapJar(project, minecraftJar, outputFile, *mappings.toTypedArray())
        return outputFile
    }

    fun computeMinecraftJar(craftExtension: BaseLegacyCraftExtension): File {
        return craftExtension.abstraction.minecraftProvider.provideMinecraftJar()
    }

}