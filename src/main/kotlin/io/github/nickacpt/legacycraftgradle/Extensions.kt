package io.github.nickacpt.legacycraftgradle

import io.github.nickacpt.legacycraftgradle.config.ClientVersion
import io.github.nickacpt.legacycraftgradle.config.LegacyCraftExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.zeroturnaround.zip.ByteSource
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

val Project.legacyCraftExtension: LegacyCraftExtension
    get() = extensions.getByType(LegacyCraftExtension::class)

fun Project.evaluate(code: Project.() -> Unit) {
    if (project.state.executed) {
        project.code()
    } else {
        project.afterEvaluate(code)
    }
}

val Project.cacheFolder: File
    get() {
        return File(gradle.gradleUserHomeDir, "caches" + File.separatorChar + "legacy-minecraft")
    }

fun Project.getCacheFolder(version: ClientVersion): File {
    return File(cacheFolder, version.toString())
}

fun Project.getCacheFile(name: String): File {
    return File(cacheFolder, name)
}

fun Project.getCacheFile(version: ClientVersion, name: String): File {
    return File(getCacheFolder(version), name)
}

fun Project.resolveClasspathPath(): List<Path> {
    return project.configurations.filter { it.isCanBeResolved }
        .flatMap { conf -> conf.resolve().map { it.toPath() } }
}

fun Project.resolveClasspathFile(): List<File> {
    return project.configurations.filter { it.isCanBeResolved }
        .flatMap { conf -> conf.resolve() }
}

fun mergeZip(output: File, input: File, condition: (ZipEntry) -> Boolean = {true}) {
    ZipFile(input).use { zip ->
        val newEntries = zip.entries().iterator().asSequence().mapNotNull { zipEntry ->
            if (!condition(zipEntry)) return@mapNotNull null
            ByteSource(zipEntry.name, zip.getInputStream(zipEntry).use { it.readAllBytes() }, zipEntry.time)
        }.toList()

        ZipUtil.addOrReplaceEntries(output, *newEntries.toTypedArray())
    }
}