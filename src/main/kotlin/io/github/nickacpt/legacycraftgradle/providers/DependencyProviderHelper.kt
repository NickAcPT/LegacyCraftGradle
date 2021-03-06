package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.create
import java.io.File

fun Project.provideDependency(group: String, name: String, version: String, file: File) {
    val providedDependency = project.dependencies.create(
        group,
        name,
        version
    )

    val outFolder = project.getCacheFile("dependency").also { it.mkdirs() }

    val depOutFile = File(outFolder, "$name-$version.jar")
    file.copyTo(depOutFile.also { it.delete() }, true)

    project.legacyCraftExtension.minecraftDependencyLocation = depOutFile

    project.repositories.flatDir { it.dir(depOutFile.parentFile) }

    project.dependencies.add(
        JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
        providedDependency
    )
}