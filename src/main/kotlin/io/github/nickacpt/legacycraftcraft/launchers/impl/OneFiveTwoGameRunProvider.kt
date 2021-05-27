package io.github.nickacpt.legacycraftcraft.launchers.impl

import io.github.nickacpt.legacycraftcraft.launchers.GameRunProvider
import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
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
        val minecraftProvider = project.legacyCraftExtension.minecraftProvider
        runClientTask.classpath(
            project.legacyCraftExtension.launchWrapperConfiguration.resolve(),
            classPath.filterNot { it == minecraftProvider.provide() },
            applyMixinsTask.output
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
            "\"-Djava.library.path=${nativesDir.absolutePath}\""
        )

        runClientTask.dependsOn(applyMixinsTask)

    }
}