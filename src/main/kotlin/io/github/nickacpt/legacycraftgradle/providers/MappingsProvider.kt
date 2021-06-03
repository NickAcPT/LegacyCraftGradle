package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.utils.DownloadUtil
import io.github.nickacpt.legacycraftgradle.utils.RemapMappingFile
import org.gradle.api.Project
import java.net.URL

class MappingsProvider(val project: Project) {

    fun getMappingsForVersion(): List<RemapMappingFile> {
        val mappings = project.legacyCraftExtension.abstraction.getMappingUrls()

        return mappings.mapIndexed { index, it ->
            val mappingsFile = project.getCacheFile("mappings-$index.tinyv2")
            DownloadUtil.downloadIfChanged(URL(it), mappingsFile, project.logger)
            RemapMappingFile(mappingsFile, "official", "named")
        }
    }

}