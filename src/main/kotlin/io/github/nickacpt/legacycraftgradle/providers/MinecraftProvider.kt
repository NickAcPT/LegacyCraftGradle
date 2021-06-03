package io.github.nickacpt.legacycraftgradle.providers

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.nickacpt.legacycraftgradle.LegacyCraftPlugin
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.mergeZip
import io.github.nickacpt.legacycraftgradle.providers.minecraft.ManifestVersion
import io.github.nickacpt.legacycraftgradle.providers.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftgradle.utils.DownloadUtil.downloadIfChanged
import io.github.nickacpt.legacycraftgradle.utils.HashedDownloadUtil
import io.github.nickacpt.legacycraftgradle.utils.VERSION_MANIFESTS
import io.github.nickacpt.legacycraftgradle.utils.remapJar
import org.gradle.api.Project
import java.io.File
import java.net.URL


class MinecraftProvider(val project: Project) {
    val version = project.legacyCraftExtension.version

    val versionManifestJson = project.getCacheFile("version_manifest.json")
    val minecraftVersionJson = project.getCacheFile(version, "version.json")
    val minecraftJar = project.getCacheFile(version, "minecraft.jar")
    val minecraftMappedJar = project.getCacheFile(version, "minecraft-mapped.jar")
    lateinit var minecraftVersionMeta: MinecraftVersionMeta

    fun provide() {
        val craftExtension = project.legacyCraftExtension
        val outputFile = getFinalMinecraftJar()

        project.provideDependency("net.minecraft", "minecraft", craftExtension.version.launcherId, outputFile)
    }

    private fun getFinalMinecraftJar(): File {
        val craftExtension = project.legacyCraftExtension
        val clientVersion = craftExtension.version
        val mapper = craftExtension.mapper

        downloadIfChanged(URL(VERSION_MANIFESTS), versionManifestJson, project.logger)
        val versionManifest = mapper.readValue<ManifestVersion>(versionManifestJson)

        val version = versionManifest.versions.first { it.id == clientVersion.launcherId }

        val url = version.url

        if (version.sha1 != null) {
            HashedDownloadUtil.downloadIfInvalid(URL(url), minecraftVersionJson, version.sha1 ?: "", project.logger, true)
        } else {
            // Use the etag if no hash found from url
            downloadIfChanged(URL(url), minecraftVersionJson, project.logger)
        }

        minecraftVersionMeta = mapper.readValue(minecraftVersionJson)

        if (!LegacyCraftPlugin.refreshDeps && minecraftMappedJar.exists()) {
            return minecraftMappedJar
        }

        val clientDownload = minecraftVersionMeta.getDownload("client") ?: throw Exception("Invalid manifest: No client download section on jar metadata")

        HashedDownloadUtil.downloadIfInvalid(
            URL(clientDownload.url),
            minecraftJar,
            clientDownload.sha1 ?: "",
            project.logger,
            false
        )

        val mappings = craftExtension.mappingsProvider.getMappingsForVersion(clientVersion)
        val outputFile = minecraftMappedJar

        val jarMods = clientVersion.getJarModUrlsToApply() + craftExtension.extraJarMods

        jarMods.forEachIndexed { index, jarModUrl ->
            val modFile = project.getCacheFile(clientVersion, "jarmod-$index.jar")
            downloadIfChanged(URL(jarModUrl), modFile, project.logger)
            if (modFile.exists())
                mergeZip(minecraftJar, modFile)
        }

        println(":mapping - Remapping Minecraft $clientVersion")
        remapJar(project, minecraftJar, outputFile, mappings)
        return outputFile
    }

}