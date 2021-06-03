package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.launchers

import org.gradle.api.tasks.JavaExec
import java.io.File

interface GameRunProvider {

    fun registerRunExtension(
        runClientTask: JavaExec,
        nativesDir: File,
        gameDir: File,
        assetsDir: File,
        classPath: MutableSet<File>
    )

}