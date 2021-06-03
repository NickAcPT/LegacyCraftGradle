package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.nickacpt.legacycraftgradle.abstraction.GameAbstractionMinecraftProvider
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.minecraft.ManifestVersion
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.mergeZip
import io.github.nickacpt.legacycraftgradle.utils.DownloadUtil
import io.github.nickacpt.legacycraftgradle.utils.HashedDownloadUtil
import io.github.nickacpt.legacycraftgradle.utils.VERSION_MANIFESTS
import java.io.File
import java.net.URL

class VanillaMinecraftProvider(val vanillaGameVersionImpl: VanillaGameVersionImpl) : GameAbstractionMinecraftProvider {

    lateinit var minecraftVersionMeta: MinecraftVersionMeta

    override fun provideMinecraftJar(): File {
        val project = vanillaGameVersionImpl.project
        val craftExtension = project.legacyCraftExtension as VanillaLegacyCraftExtension

        val versionManifestJson = project.getCacheFile("version_manifest.json")
        val minecraftVersionJson = project.getCacheFile("version.json")
        val minecraftJar = project.getCacheFile("minecraft.jar")
        val clientVersion = craftExtension.version
        val mapper = craftExtension.mapper


        DownloadUtil.downloadIfChanged(URL(VERSION_MANIFESTS), versionManifestJson, project.logger)
        val versionManifest = mapper.readValue<ManifestVersion>(versionManifestJson)

        val version = versionManifest.versions.first { it.id == clientVersion.launcherId }

        val url = version.url

        if (version.sha1 != null) {
            HashedDownloadUtil.downloadIfInvalid(
                URL(url),
                minecraftVersionJson,
                version.sha1 ?: "",
                project.logger,
                true
            )
        } else {
            // Use the etag if no hash found from url
            DownloadUtil.downloadIfChanged(URL(url), minecraftVersionJson, project.logger)
        }

        minecraftVersionMeta = mapper.readValue(minecraftVersionJson)

        val clientDownload = minecraftVersionMeta.getDownload("client")
            ?: throw Exception("Invalid manifest: No client download section on jar metadata")

        HashedDownloadUtil.downloadIfInvalid(
            URL(clientDownload.url),
            minecraftJar,
            clientDownload.sha1 ?: "",
            project.logger,
            false
        )

        val jarMods = clientVersion.getJarModUrlsToApply() + craftExtension.extraJarMods

        jarMods.forEachIndexed { index, jarModUrl ->
            val modFile = project.getCacheFile("jarmod-$index.jar")
            DownloadUtil.downloadIfChanged(URL(jarModUrl), modFile, project.logger)
            if (modFile.exists())
                mergeZip(minecraftJar, modFile)
        }

        return minecraftJar

    }
}