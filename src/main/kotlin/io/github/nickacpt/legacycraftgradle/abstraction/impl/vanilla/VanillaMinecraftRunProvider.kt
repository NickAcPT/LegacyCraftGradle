package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.abstraction.GameAbstractionMinecraftRunProvider
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers.GameRunProvider
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers.impl.VanillaGameRunProvider
import io.github.nickacpt.legacycraftgradle.config.ClientVersion
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.maven
import java.io.File

class VanillaMinecraftRunProvider(val impl: VanillaGameVersionImpl) : GameAbstractionMinecraftRunProvider {
    val project get() = impl.project
    override fun provideRun() {
        val extension = project.legacyCraftExtension as VanillaLegacyCraftExtension
        addLegacyLauncher(extension.version)

        val gameRunProvider = getRunProvider(extension.version)

        val (nativesDir, _) = impl.nativesProvider.getNativeFileDirs()
        val gameDir = File(project.projectDir, "run").also { it.mkdirs() }

        extension.assetsProvider.provide()
        val assetsDir = project.getCacheFile("assets")

        val classPath = project.legacyCraftExtension.minecraftLibConfiguration.resolve()

        if (gameRunProvider != null) {
            val runClientTask = project.tasks.create<JavaExec>("runClient") {}
            gameRunProvider.registerRunExtension(runClientTask, nativesDir, gameDir, assetsDir, classPath)
        }
    }

    private fun addLegacyLauncher(version: ClientVersion) {
        val wrapperConfiguration = "launchWrapper"
        val launchWrapperConfiguration = project.configurations.create(wrapperConfiguration) {
            project.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(it)
        }
        project.legacyCraftExtension.launchWrapperConfiguration = launchWrapperConfiguration

        val launcherVersion =
            when (version) {
                ClientVersion.ONE_FIVE_TWO -> "ed6d4b98f1"
                ClientVersion.ONE_EIGHT_NINE -> "eeb97beb2d"
            }

        project.repositories.maven("https://jitpack.io")

        val legacyLauncher = project.dependencies.add(
            wrapperConfiguration,
            "com.github.NickAcPT:LegacyLauncher:$launcherVersion"
        ) as ExternalModuleDependency
        legacyLauncher.exclude(module = "lwjgl")
    }

    private fun getRunProvider(version: ClientVersion): GameRunProvider? {
        return VanillaGameRunProvider(project, version)
    }

}