package io.github.nickacpt.legacycraftcraft.providers

import io.github.nickacpt.legacycraftcraft.config.ClientVersion
import io.github.nickacpt.legacycraftcraft.launchers.GameRunProvider
import io.github.nickacpt.legacycraftcraft.launchers.impl.OneFiveTwoGameRunProvider
import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import java.io.File

class MinecraftLaunchProvider(val project: Project) {

    fun provide() {
        val extension = project.legacyCraftExtension
        if (extension.version == ClientVersion.ONE_FIVE_TWO) {
            addLegacyLauncher()
        }
        val gameRunProvider = getRunProvider(extension.version)

        val (nativesDir, _) = extension.nativesProvider.getNativeFileDirs()
        val gameDir = File(project.projectDir, "run").also { it.mkdirs() }
        val assetsDir = File(gameDir, "assets")

        val classPath = project.legacyCraftExtension.minecraftLibConfiguration.resolve()

        if (gameRunProvider != null) {
            val runClientTask = project.tasks.create<JavaExec>("runClient")
            gameRunProvider.registerRunExtension(runClientTask, nativesDir, gameDir, assetsDir, classPath)
        }
    }

    private fun addLegacyLauncher() {
        val launchWrapperConfiguration = project.configurations.create("launchWrapper")
       project.legacyCraftExtension.launchWrapperConfiguration = launchWrapperConfiguration

        val launcherVersion = "aff3a537ee"
        project.repositories.maven("https://jitpack.io")

        project.dependencies.add(
            "launchWrapper",
            "com.github.sp614x:LegacyLauncher:$launcherVersion"
        )
    }

    private fun getRunProvider(version: ClientVersion): GameRunProvider? {
        return when (version) {
            ClientVersion.ONE_FIVE_TWO -> OneFiveTwoGameRunProvider(project)
            else -> null
        }
    }

}