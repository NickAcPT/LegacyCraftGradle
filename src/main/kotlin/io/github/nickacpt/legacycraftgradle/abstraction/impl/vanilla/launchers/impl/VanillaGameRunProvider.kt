package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers.impl

import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.VanillaMinecraftLibrariesProvider.Companion.mixinDependency
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers.GameRunProvider
import io.github.nickacpt.legacycraftgradle.config.ClientVersion
import io.github.nickacpt.legacycraftgradle.getJarTask
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.tasks.ApplyMixinsTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import java.io.File

class VanillaGameRunProvider(val project: Project, val version: ClientVersion) : GameRunProvider {
    override fun registerRunExtension(
        runClientTask: JavaExec,
        nativesDir: File,
        gameDir: File,
        assetsDir: File,
        classPath: MutableSet<File>
    ) {
        val extension = project.legacyCraftExtension

        val applyMixinsTask = extension.applyMixinsTask
        runClientTask.classpath(
            *getJarFileToRun(applyMixinsTask),
            extension.launchWrapperConfiguration.resolve(),
            classPath.filterNot { it == extension.minecraftDependencyLocation }
        )
        runClientTask.workingDir = gameDir
        runClientTask.mainClass.set("net.minecraft.launchwrapper.Launch")
        runClientTask.args = (if (version == ClientVersion.ONE_FIVE_TWO) emptyList() else listOf(
            "--version",
            project.name,

            "--accessToken",
            "0",

            "--userProperties",
            "{}",
        )) + mutableListOf(
            "Developer", /* Name */

            "--gameDir", /* Game Dir */
            "\"${gameDir.absolutePath}\"", /* Game Dir */

            "--assetsDir", /* Assets Dir */
            "\"${assetsDir.absolutePath}\"", /* Assets Dir */

            "--tweakClass", /* Vanilla Tweaker class */
            "net.minecraft.launchwrapper.VanillaTweaker", /* Vanilla Tweaker class */
        ).also { list ->
            if (extension.runMixinWithTweaker) {
                // Add Mixin tweaker
                list.add("--tweakClass")
                var mixinTweakerClass = "org.spongepowered.asm.launch.MixinTweaker"
                if (version == ClientVersion.ONE_FIVE_TWO) {
                    mixinTweakerClass = "org.spongepowered.asm.launch.LegacyMixinTweaker"
                }
                list.add(mixinTweakerClass)

                val configurations = ApplyMixinsTask.getMixinFiles(project).map { it.name }
                configurations.forEach {
                    list.add("--mixin")
                    list.add(it)
                }
            }
        }

        runClientTask.jvmArgs = mutableListOf(
            "\"-Djava.library.path=${nativesDir.absolutePath}\"",
            "-Dlegacycraft.launch=true"
        ).also {
            if (extension.runDeobfuscatedClient) {
                // Resolve Mixins Jar and add as java agent for Mixin Hotswap
                val mixinsJarConfig = project.configurations.detachedConfiguration()
                mixinsJarConfig.dependencies.add(project.dependencies.create(mixinDependency))

                val mixinsJar = mixinsJarConfig.resolve().first()

                it.add("-javaagent:" + mixinsJar.absolutePath)
            }
        }

        // Running Mixins with a Tweaker will no longer require us to pre-apply Mixins
        var taskToDepend: Task = applyMixinsTask
        if (extension.runMixinWithTweaker) {
            taskToDepend = project.getJarTask()
        }

        runClientTask.dependsOn(taskToDepend)

    }

    private fun getJarFileToRun(applyMixinsTask: ApplyMixinsTask): Array<File> {
        val extension = project.legacyCraftExtension
        // When running with the tweaker, Minecraft will be launched deobfuscated and mixins will be applied at runtime
        if (extension.runMixinWithTweaker) {
            return arrayOf(
                extension.minecraftProvider.minecraftMappedJar,
                *project.getJarTask().outputs.files.files.toTypedArray()
            )
        }
        return arrayOf(applyMixinsTask.output)
    }
}