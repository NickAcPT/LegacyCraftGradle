package io.github.nickacpt.legacycraftgradle

import io.github.nickacpt.legacycraftgradle.config.LegacyCraftExtension
import io.github.nickacpt.legacycraftgradle.providers.*
import io.github.nickacpt.legacycraftgradle.tasks.ApplyMixinsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create

class LegacyCraftPlugin : Plugin<Project> {
    companion object {
        var refreshDeps: Boolean = false
    }

    override fun apply(project: Project) {
        refreshDeps = project.gradle.startParameter.isRefreshDependencies
        val extension = project.extensions.create("legacycraft", LegacyCraftExtension::class)

        project.evaluate {
            with(extension) {
                mappingsProvider = MappingsProvider(project)
                minecraftProvider = MinecraftProvider(project)
                minecraftLibraryProvider = MinecraftLibraryProvider(project)
                mixinProvider = MixinProvider(project)
                nativesProvider = MinecraftNativesProvider(project)
                launchProvider = MinecraftLaunchProvider(project)
                assetsProvider = MinecraftAssetsProvider(project)
            }

            val version = extension.version
            println("LegacyCraftGradle - Minecraft Version ${version.launcherId}")

            extension.minecraftProvider.provide()
            extension.minecraftLibraryProvider.provide()
            extension.mixinProvider.provide()
            extension.nativesProvider.provide()

            val jarTask = getJarTask().apply {
                (this as? Jar)?.archiveClassifier?.set("dev")
            }
            val applyMixins = tasks.create("applyMixins", ApplyMixinsTask::class).apply {
                input = jarTask.outputs.files.first()
            }

            extension.applyMixinsTask = applyMixins
            applyMixins.dependsOn(jarTask)

            extension.launchProvider.provide()

            jarTask.finalizedBy(applyMixins)
            tasks.getByName("assemble").finalizedBy(applyMixins)
        }
    }

    private fun Project.getJarTask() = kotlin.runCatching { tasks.getByName("shadowJar") }.getOrNull() ?: tasks.getByName("jar")
}