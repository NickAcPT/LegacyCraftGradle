package io.github.nickacpt.legacycraftcraft.launchers.impl

import io.github.nickacpt.legacycraftcraft.launchers.GameRunProvider
import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
import io.github.nickacpt.legacycraftcraft.tasks.ApplyMixinsTask
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import java.io.File

class OneFiveTwoGameRunProvider(val project: Project) : GameRunProvider {
    override fun registerRunExtension(
        runClientTask: JavaExec,
        nativesDir: File,
        gameDir: File,
        assetsDir: File,
        classPath: List<File>
    ) {
        val applyMixinsTask = project.tasks.getByName("applyMixins") as ApplyMixinsTask
        val minecraftProvider = project.legacyCraftExtension.minecraftProvider
        runClientTask.classpath(
            classPath.filterNot { it.nameWithoutExtension == "launchwrapper-1.5" && it == minecraftProvider.minecraftMappedJar },
            classPath.filter { it.nameWithoutExtension == "launchwrapper-1.5" },
            applyMixinsTask.output
        )
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