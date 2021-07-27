package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.BaseLegacyCraftPlugin
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.utils.RemapMappingFile
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import org.gradle.api.Project
import java.net.URL


class MappingsProvider(val project: Project) {

    fun getMappingsForVersion(): RemapMappingFile {
        val finalFile = project.getCacheFile("mappings-final.tinyv2")
        val remapMappingFile = RemapMappingFile(finalFile, "official", "named")
        val mappings = project.legacyCraftExtension.abstraction.getMappingUrls()
        if (finalFile.exists() && !BaseLegacyCraftPlugin.refreshDeps) return remapMappingFile

        val mergedMappings = MemoryMappingTree() as MappingTree

        // Load and merge our mappings
        mappings.forEachIndexed { i, url ->
            val file = project.getCacheFile("mappings-$i")
            file.writeBytes(URL(url).readBytes())

            MappingReader.read(file.toPath(), MappingReader.detectFormat(file.toPath()), mergedMappings as MappingVisitor)
        }

        val namedId = mergedMappings.getNamespaceId("named")

        // Remove un-named entries from final map
        mergedMappings.classes.removeIf { c -> c.getDstName(namedId) == null }
        mergedMappings.classes.forEach { c ->
            c.fields.removeIf { value -> value.getDstName(namedId) == null }
            c.methods.removeIf { value -> value.getDstName(namedId) == null }
        }

        MappingWriter.create(finalFile.toPath(), MappingFormat.TINY_2).use {
            mergedMappings.accept(it)
        }

        return remapMappingFile
    }

}