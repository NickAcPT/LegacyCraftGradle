package io.github.nickacpt.legacycraftcraft.providers

import io.github.nickacpt.legacycraftcraft.config.ClientVersion
import io.github.nickacpt.legacycraftcraft.utils.RemapMappingFile
import org.gradle.api.Project
import java.io.File

class MappingsProvider(val project: Project) {

    fun getMappingsForVersion(version: ClientVersion): RemapMappingFile? {
        return when (version) {
            ClientVersion.ONE_FIVE_TWO -> RemapMappingFile(File("""R:\Work\client.tinyv2"""), "official", "mcp")
            ClientVersion.ONE_EIGHT_NINE -> RemapMappingFile(File("""R:\Work\1.8.9\client.tinyv2"""), "official", "mcp")
            else -> null
        }
    }

}