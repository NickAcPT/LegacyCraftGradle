package io.github.nickacpt.legacycraftgradle.launchers.impl

import io.github.nickacpt.legacycraftgradle.launchers.GameRunProvider
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import java.io.File

class OneFiveTwoGameRunProvider(val project: Project) : GameRunProvider {
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
            classPath.filterNot { it == project.legacyCraftExtension.minecraftDependencyLocation || it.nameWithoutExtension == "launchwrapper-1.5" }
        )
        runClientTask.workingDir = gameDir
        runClientTask.mainClass.set("net.minecraft.launchwrapper.Launch")
        runClientTask.args = listOf(
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