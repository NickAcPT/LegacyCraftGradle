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
package io.github.nickacpt.legacycraftgradle.providers

import com.google.common.base.Stopwatch
import io.github.nickacpt.legacycraftgradle.config.LegacyCraftExtension
import io.github.nickacpt.legacycraftgradle.getCacheFile
import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.providers.minecraft.MinecraftVersionMeta
import io.github.nickacpt.legacycraftgradle.providers.minecraft.assets.AssetIndex
import io.github.nickacpt.legacycraftgradle.providers.minecraft.assets.AssetObject
import io.github.nickacpt.legacycraftgradle.utils.HashedDownloadUtil
import io.github.nickacpt.legacycraftgradle.utils.ProgressLogger
import io.github.nickacpt.legacycraftgradle.utils.RESOURCES_BASE
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MinecraftAssetsProvider(val project: Project) {
    @Throws(IOException::class)
    fun provide() {
        val extension: LegacyCraftExtension = project.legacyCraftExtension
        val offline = project.gradle.startParameter.isOffline
        val versionInfo: MinecraftVersionMeta = extension.minecraftProvider.minecraftVersionMeta
        val assetIndex: MinecraftVersionMeta.AssetIndex = versionInfo.assetIndex ?: return

        // get existing cache files
        var assets: File = project.getCacheFile(extension.version, "assets")
        if (!assets.exists()) {
            assets.mkdirs()
        }
        val assetsInfo = File(
            assets,
            "indexes" + File.separator + assetIndex.id + ".json"
        )
        project.logger.info(":downloading asset index")
        if (offline) {
            if (assetsInfo.exists()) {
                //We know it's outdated but can't do anything about it, oh well
                project.logger.warn("Asset index outdated")
            } else {
                //We don't know what assets we need, just that we don't have any
                throw GradleException("Asset index not found at " + assetsInfo.absolutePath)
            }
        } else {
            HashedDownloadUtil.downloadIfInvalid(
                URL(assetIndex.url),
                assetsInfo,
                assetIndex.sha1,
                project.logger,
                false
            )
        }
        val loggers: Deque<ProgressLogger> = ConcurrentLinkedDeque()
        val executor: ExecutorService =
            Executors.newFixedThreadPool(
                10.coerceAtMost(
                    (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(
                        1
                    )
                )
            )
        var index: AssetIndex
        FileReader(assetsInfo).use { fileReader ->
            index = extension.mapper.readValue(fileReader, AssetIndex::class.java)
        }

        if (index.isMapToResources) {
            assets = File(project.projectDir, "run" + File.separatorChar + "resources").also { it.mkdirs() }
        }

        val stopwatch = Stopwatch.createStarted()
        val parent: Map<String, AssetObject> = index.fileMap
        for ((key, `object`) in parent) {
            val sha1: String = `object`.hash ?: ""
            val filename = if (index.isMapToResources) key else "objects" + File.separator + sha1.substring(0, 2) + File.separator + sha1
            val file = File(assets, filename)
            if (offline) {
                if (file.exists()) {
                    project.logger.warn("Outdated asset $key")
                } else {
                    throw GradleException("Asset " + key + " not found at " + file.absolutePath)
                }
            } else {
                executor.execute {
                    val assetName = arrayOf(key)
                    val end = assetName[0].lastIndexOf("/") + 1
                    if (end > 0) {
                        assetName[0] = assetName[0].substring(end)
                    }
                    project.logger.debug("validating asset " + assetName[0])
                    val progressLogger: Array<ProgressLogger?> = arrayOfNulls(1)
                    try {
                        HashedDownloadUtil.downloadIfInvalid(
                            URL(
                                RESOURCES_BASE + sha1.substring(
                                    0,
                                    2
                                ) + "/" + sha1
                            ), file, sha1, project.logger, true
                        ) {
                            val logger = loggers.pollFirst()
                            if (logger == null) {
                                //Create a new logger if we need one
                                progressLogger[0] =
                                    ProgressLogger.getProgressFactory(project, MinecraftAssetsProvider::class.java.name)
                                progressLogger[0]?.start("Downloading assets...", "assets")
                            } else {
                                // use a free logger if we can
                                progressLogger[0] = logger
                            }
                            project.logger.debug("downloading asset " + assetName[0])
                            progressLogger[0]?.progress(String.format("%-30.30s", assetName[0]) + " - " + sha1)
                        }
                    } catch (e: IOException) {
                        throw RuntimeException("Failed to download: " + assetName[0], e)
                    }
                    if (progressLogger[0] != null) {
                        //Give this logger back if we used it
                        loggers.add(progressLogger[0])
                    }
                }
            }
        }
        project.logger.info("Took " + stopwatch.stop() + " to iterate " + parent.size + " asset index.")

        //Wait for the assets to all download
        executor.shutdown()
        try {
            if (executor.awaitTermination(2, TimeUnit.HOURS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        loggers.forEach(ProgressLogger::completed)
    }
}