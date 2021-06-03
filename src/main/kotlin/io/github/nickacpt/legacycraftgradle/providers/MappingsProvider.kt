package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.config.ClientVersion
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.utils.DownloadUtil
import io.github.nickacpt.legacycraftgradle.utils.RemapMappingFile
import org.gradle.api.Project
import java.net.URL

class MappingsProvider(val project: Project) {

    private fun getCacheFileForVersion(version: ClientVersion) = project.getCacheFile(version, "mappings.tinyv2")

    fun getMappingsForVersion(version: ClientVersion): RemapMappingFile? {
        val url = "https://raw.githubusercontent.com/NickAcPT/LegacyCraftMappings/main/$version/client.tinyv2"

        val mappingsFile = getCacheFileForVersion(version)
        DownloadUtil.downloadIfChanged(URL(url), mappingsFile, project.logger)

        return RemapMappingFile(mappingsFile, "official", "mcp")
    }

}