package io.github.nickacpt.legacycraftcraft.providers

import io.github.nickacpt.legacycraftcraft.getCacheFile
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.create
import java.io.File

fun Project.provideDependency(group: String, name: String, version: String, file: File): File {
    val providedDependency = project.dependencies.create(
        group,
        name,
        version
    )

    val outFolder = project.getCacheFile(version).also { it.mkdirs() }

    val depOutFile = File(outFolder, "$name-$version.jar")
    file.copyTo(depOutFile.also { it.delete() }, true)

    project.repositories.flatDir { it.dir(depOutFile.parentFile) }

    project.dependencies.add(
        JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
        providedDependency
    )

    return depOutFile
}