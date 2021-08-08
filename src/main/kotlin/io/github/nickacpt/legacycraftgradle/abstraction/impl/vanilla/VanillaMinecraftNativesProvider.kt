package io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla

import io.github.nickacpt.legacycraftgradle.BaseLegacyCraftPlugin
import io.github.nickacpt.legacycraftgradle.abstraction.GameAbstractionMinecraftNativesProvider
import io.github.nickacpt.legacycraftgradle.abstraction.impl.vanilla.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.isOffline
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.utils.HashedDownloadUtil
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets

class VanillaMinecraftNativesProvider(val impl: VanillaGameVersionImpl) : GameAbstractionMinecraftNativesProvider {
    val project
        get() = impl.project

    override fun provideMinecraftNatives() {
        if (!BaseLegacyCraftPlugin.refreshDeps && !requiresExtract()) {
            project.logger.info("Natives do no need extracting, skipping")
            return
        }
        extractNatives()
    }

    @Throws(IOException::class)
    private fun extractNatives() {
        val (nativesDir, jarStore) = getNativeFileDirs()

        val offline = project.isOffline
        if (nativesDir.exists()) {
            try {
                FileUtils.deleteDirectory(nativesDir)
            } catch (e: IOException) {
                throw IOException("Failed to delete the natives directory, is the game running?", e)
            }
        }
        nativesDir.mkdirs()
        for (library in natives) {
            val libJarFile: File = library.relativeFile(jarStore)
            if (!offline) {
                HashedDownloadUtil.downloadIfInvalid(
                    URL(library.url),
                    libJarFile,
                    library.sha1,
                    project.logger,
                    false
                )
            }
            if (!libJarFile.exists()) {
                throw GradleException("Native jar not found at " + libJarFile.absolutePath)
            }
            ZipUtil.unpack(libJarFile, nativesDir)

            // Store a file containing the hash of the extracted natives, used on subsequent runs to skip extracting all the natives if they haven't changed
            val libSha1File = File(nativesDir, libJarFile.name + ".sha1")
            libSha1File.writeText(library.sha1 ?: "")
        }
    }

    fun getNativeFileDirs(): Pair<File, File> {
        val extension = project.legacyCraftExtension
        val nativesDir = project.getCacheFile("natives").also { it.mkdirs() }
        val jarStore = project.getCacheFile("natives-jarstore").also { it.mkdirs() }
        return Pair(nativesDir, jarStore)
    }

    private fun requiresExtract(): Boolean {
        val (nativesDir, jarStore) = getNativeFileDirs()
        val natives = natives
        check(natives.isNotEmpty()) { "No natives found for the current system" }
        for (library in natives) {
            val libJarFile: File = library.relativeFile(jarStore)
            val libSha1File = File(nativesDir, libJarFile.name + ".sha1")
            if (!libSha1File.exists()) {
                return true
            }
            try {
                val sha1 = FileUtils.readFileToString(libSha1File, StandardCharsets.UTF_8)
                if (!sha1.equals(library.sha1, ignoreCase = true)) {
                    return true
                }
            } catch (e: IOException) {
                project.logger.error("Failed to read " + libSha1File.absolutePath, e)
                return true
            }
        }

        // All looks good, no need to re-extract
        return false
    }

    private val natives: List<MinecraftVersionMeta.Download>
        get() = impl.minecraftProvider.minecraftVersionMeta.libraries
            ?.filter { it.hasNativesForOS() }
            ?.mapNotNull { it.classifierForOS } ?: emptyList()

}