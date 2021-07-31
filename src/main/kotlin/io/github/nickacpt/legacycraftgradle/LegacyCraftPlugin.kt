package io.github.nickacpt.legacycraftgradle

import io.github.nickacpt.legacycraftgradle.abstraction.GameVersionAbstraction
import io.github.nickacpt.legacycraftgradle.providers.*
import io.github.nickacpt.legacycraftgradle.tasks.ApplyMixinsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create

abstract class BaseLegacyCraftPlugin : Plugin<Project> {
    companion object {
        var refreshDeps: Boolean = false
    }

    lateinit var abstraction: GameVersionAbstraction<*>

    override fun apply(project: Project) {
        refreshDeps = project.gradle.startParameter.isRefreshDependencies
        abstraction.project = project
        abstraction.init()
        val extension = abstraction.getExtension()
        extension.abstraction = abstraction

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

            val version = extension.abstraction.getVersionName()
            println("LegacyCraftGradle - Minecraft $version")

            extension.minecraftProvider.provide()
            extension.minecraftLibraryProvider.provide()
            extension.mixinProvider.provide()
            extension.nativesProvider.provide()

            val jarTask = getJarTask().apply {
                (this as? Jar)?.archiveClassifier?.set("dev")
            }
            val applyMixins = tasks.create("applyMixins", ApplyMixinsTask::class).apply {
                inputFunc = { jarTask.outputs.files.first() }
            }

            extension.applyMixinsTask = applyMixins
            applyMixins.dependsOn(jarTask)

            extension.launchProvider.provide()

            tasks.getByName("assemble").finalizedBy(applyMixins)
        }
    }

}

fun Project.getJarTask() = kotlin.runCatching { tasks.getByName("shadowJar") }.getOrNull() ?: tasks.getByName("jar")
