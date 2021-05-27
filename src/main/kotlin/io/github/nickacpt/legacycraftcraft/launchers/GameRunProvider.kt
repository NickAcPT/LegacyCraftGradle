package io.github.nickacpt.legacycraftcraft.launchers

import org.gradle.api.tasks.JavaExec
import java.io.File

interface GameRunProvider {

    fun registerRunExtension(
        runClientTask: JavaExec,
        nativesDir: File,
        gameDir: File,
        assetsDir: File,
        classPath: List<File>
    )

}