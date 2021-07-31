package io.github.nickacpt.legacycraftgradle

import io.github.nickacpt.legacycraftgradle.config.BaseLegacyCraftExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.zeroturnaround.zip.ByteSource
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

val Project.legacyCraftExtension: BaseLegacyCraftExtension
    get() = extensions.getByType(BaseLegacyCraftExtension::class)

fun Project.evaluate(code: Project.() -> Unit) {
    if (project.state.executed) {
        project.code()
    } else {
        project.afterEvaluate(code)
    }
}

val Project.legacyCraftCacheFolder: File
    get() {
        return File(gradle.gradleUserHomeDir, "caches" + File.separatorChar + "legacy-minecraft")
    }

fun Project.getCacheFolder(): File {
    return File(legacyCraftCacheFolder, (project.legacyCraftExtension.abstraction.getVersionName()))
}

fun Project.getCacheFile(name: String): File {
    return File(getCacheFolder(), name)
}

fun Project.resolveClasspathPath(): List<Path> {
    return project.configurations.filter { it.isCanBeResolved }
        .flatMap { conf -> conf.resolve().map { it.toPath() } }
}

fun Project.resolveClasspathFile(): List<File> {
    return project.configurations.filter { it.isCanBeResolved }
        .flatMap { conf -> conf.resolve() }
}

fun mergeZip(
    output: File,
    input: File,
    classPrefix: String = "",
    condition: (ZipEntry) -> Boolean = { true }
) {
    mergeZip(output, input, classPrefix, condition) { _, bytes -> bytes }
}

fun mergeZip(
    output: File,
    input: File,
    classPrefix: String = "",
    condition: (ZipEntry) -> Boolean = { true },
    byteMapper: (ZipEntry, ByteArray) -> ByteArray
) {
    ZipFile(input).use { zip ->
        val newEntries = zip.entries().iterator().asSequence().mapNotNull { zipEntry ->
            val prefix = if (zipEntry.name.endsWith(".class")) classPrefix else ""
            if (!condition(zipEntry) || zipEntry.isDirectory) return@mapNotNull null
            ByteSource(
                prefix + zipEntry.name,
                zip.getInputStream(zipEntry).use { byteMapper(zipEntry, it.readAllBytes()) },
                zipEntry.time
            )
        }.toList()

        ZipUtil.addOrReplaceEntries(output, *newEntries.toTypedArray())
    }
}