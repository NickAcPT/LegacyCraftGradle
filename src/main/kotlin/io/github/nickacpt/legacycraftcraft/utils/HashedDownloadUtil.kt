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
package io.github.nickacpt.legacycraftcraft.utils

import com.google.common.hash.Hashing
import com.google.common.io.Files
import io.github.nickacpt.legacycraftcraft.LegacyCraftPlugin.Companion.refreshDeps
import io.github.nickacpt.legacycraftcraft.utils.DownloadUtil.toNiceSize
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object HashedDownloadUtil {
    @JvmOverloads
    @Throws(IOException::class)
    fun downloadIfInvalid(
        from: URL,
        to: File,
        expectedHash: String?,
        logger: Logger,
        quiet: Boolean,
        startDownload: Runnable = Runnable {}
    ) {
        if (refreshDeps) {
            delete(to)
        }
        val sha1 = getSha1(to, logger)
        if (expectedHash == sha1) {
            // The hash in the sha1 file matches
            return
        }
        startDownload.run()
        val connection = from.openConnection() as HttpURLConnection
        //		connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.connect()
        val code = connection.responseCode
        if ((code < 200 || code > 299) && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            //Didn't get what we expected
            delete(to)
            throw IOException(connection.responseMessage + " for " + from)
        }
        val contentLength = connection.contentLengthLong
        if (!quiet && contentLength >= 0) {
            logger.info("'{}' Changed, downloading {}", to, toNiceSize(contentLength))
        }
        try { // Try download to the output
            FileUtils.copyInputStreamToFile(connection.inputStream, to)
        } catch (e: IOException) {
            delete(to) // Probably isn't good if it fails to copy/save
            throw e
        }
        if (expectedHash != null && !Checksum.equals(to, expectedHash)) {
            val actualHash = Files.asByteSource(to).hash(Hashing.sha1()).toString()
            delete(to)
            throw IOException(
                String.format(
                    "Downloaded file from %s to %s and got unexpected hash of %s expected %s",
                    from,
                    to,
                    actualHash,
                    expectedHash
                )
            )
        }
        saveSha1(to, expectedHash, logger)
    }

    private fun getSha1File(file: File): File {
        return File(file.absoluteFile.parentFile, file.name + ".sha1")
    }

    private fun getSha1(to: File, logger: Logger): String? {
        if (!to.exists()) {
            delete(to)
            return null
        }
        val sha1File = getSha1File(to)
        return try {
            Files.asCharSource(sha1File, StandardCharsets.UTF_8).read()
        } catch (ignored: FileNotFoundException) {
            // Quicker to catch this than do an exists check before.
            null
        } catch (e: IOException) {
            logger.warn("Error reading sha1 file '{}'.", sha1File)
            null
        }
    }

    private fun saveSha1(to: File, sha1: String?, logger: Logger) {
        val sha1File = getSha1File(to)
        try {
            if (!sha1File.exists()) {
                sha1File.createNewFile()
            }
            Files.asCharSink(sha1File, StandardCharsets.UTF_8).write(sha1 ?: "")
        } catch (e: IOException) {
            logger.warn("Error saving sha1 file '{}'.", sha1File, e)
        }
    }

    fun delete(file: File) {
        if (file.exists()) {
            file.delete()
        }
        val sha1File = getSha1File(file)
        if (sha1File.exists()) {
            sha1File.delete()
        }
    }
}