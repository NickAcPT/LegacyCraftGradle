/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.nickacpt.legacycraftcraft.providers

import io.github.nickacpt.legacycraftcraft.LegacyCraftPlugin
import io.github.nickacpt.legacycraftcraft.getCacheFile
import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftcraft.utils.HashedDownloadUtil
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets

class MinecraftNativesProvider(private val project: Project) {
    fun provide() {
        if (!LegacyCraftPlugin.refreshDeps && !requiresExtract()) {
            project.logger.info("Natives do no need extracting, skipping")
            return
        }
        extractNatives()
    }

    @Throws(IOException::class)
    private fun extractNatives() {
        val (nativesDir, jarStore) = getNativeFileDirs()

        val offline = project.gradle.startParameter.isOffline
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
            libSha1File.writeText(library.sha1)
        }
    }

    fun getNativeFileDirs(): Pair<File, File> {
        val extension = project.legacyCraftExtension
        val nativesDir = project.getCacheFile(extension.version, "natives").also { it.mkdirs() }
        val jarStore = project.getCacheFile(extension.version, "natives-jarstore").also { it.mkdirs() }
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
        get() = project.legacyCraftExtension.minecraftProvider.minecraftVersionMeta.libraries
            .filter { it.hasNativesForOS() }
            .map { it.classifierForOS }

}