package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers.impl

import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers.GameRunProvider
import io.github.nickacpt.legacycraftgradle.config.ClientVersion
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import org.gradle.api.Project
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
        val applyMixinsTask = project.legacyCraftExtension.applyMixinsTask
        runClientTask.classpath(
            if (project.legacyCraftExtension.runDeobfuscatedClient) applyMixinsTask.outputDeobfuscated else applyMixinsTask.output,
            project.legacyCraftExtension.launchWrapperConfiguration.resolve(),
            classPath.filterNot { it == project.legacyCraftExtension.minecraftDependencyLocation }
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
        )) + listOf(
            "Developer", /* Name */

            "--gameDir", /* Game Dir */
            "\"${gameDir.absolutePath}\"", /* Game Dir */

            "--assetsDir", /* Assets Dir */
            "\"${assetsDir.absolutePath}\"", /* Assets Dir */
        )


        runClientTask.jvmArgs = listOf(
            "\"-Djava.library.path=${nativesDir.absolutePath}\"",
            "-Dlegacycraft.launch=true"
        )

        runClientTask.dependsOn(applyMixinsTask)

    }
}