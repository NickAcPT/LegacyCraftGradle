package io.github.nickacpt.legacycraftgradle.utils

import io.github.nickacpt.legacycraftgradle.resolveClasspathPath
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.Project
import java.io.File
import java.io.IOException

data class RemapMappingFile(val file: File, val from: String, val to: String) {
    fun reverse(): RemapMappingFile {
        return this.copy(from = to, to = from)
    }
}

fun remapJar(
    project: Project,
    inputFile: File,
    output: File,
    mappings: RemapMappingFile?,
    resolveClassPath: Boolean = false
) {
    val remapper = TinyRemapper.newRemapper()
        .also {
            if (mappings == null) return@also
            it.withMappings(
                TinyUtils.createTinyMappingProvider(
                    mappings.file.toPath(),
                    mappings.from,
                    mappings.to
                )
            )
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .fixPackageAccess(true)
                .ignoreConflicts(true)
        }.build()

    try {
        val input = inputFile.toPath()

        OutputConsumerPath.Builder(output.toPath()).build().use { outputConsumer ->
            outputConsumer.addNonClassFiles(input, NonClassCopyMode.SKIP_META_INF, remapper)
            remapper.readInputs(input)
            if (resolveClassPath)
                remapper.readClassPath(*project.resolveClasspathPath().toTypedArray())
            remapper.apply(outputConsumer)
        }
    } catch (e: IOException) {
        throw RuntimeException(e)
    } finally {
        remapper.finish()
    }
}