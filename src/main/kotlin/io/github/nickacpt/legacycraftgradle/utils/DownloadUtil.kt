/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2019 Chocohead
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
package io.github.nickacpt.legacycraftgradle.utils

import io.github.nickacpt.legacycraftgradle.LegacyCraftPlugin
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object DownloadUtil {
    /**
     * Download from the given [URL] to the given [File] so long as there are differences between them.
     *
     * @param from The URL of the file to be downloaded
     * @param to The destination to be saved to, and compared against if it exists
     * @param logger The logger to print everything to, typically from [Project.getLogger]
     * @throws IOException If an exception occurs during the process
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun downloadIfChanged(from: URL, to: File, logger: Logger, quiet: Boolean = false) {
        val connection = from.openConnection() as HttpURLConnection
        if (LegacyCraftPlugin.refreshDeps) {
            getETagFile(to).delete()
            to.delete()
        }

        // If the output already exists we'll use it's last modified time
        if (to.exists()) {
            connection.ifModifiedSince = to.lastModified()
        }

        //Try use the ETag if there's one for the file we're downloading
        val etag = loadETag(to, logger)
        if (etag != null) {
            connection.setRequestProperty("If-None-Match", etag)
        }

//         We want to download gzip compressed stuff
//        connection.setRequestProperty("Accept-Encoding", "gzip")

        // Try make the connection, it will hang here if the connection is bad
        connection.connect()
        val code = connection.responseCode
        if ((code < 200 || code > 299) && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            //Didn't get what we expected
            delete(to)
            throw IOException(connection.responseMessage + " for " + from)
        }
        val modifyTime = connection.getHeaderFieldDate("Last-Modified", -1)
        if (to.exists() && (code == HttpURLConnection.HTTP_NOT_MODIFIED || modifyTime > 0 && to.lastModified() >= modifyTime)) {
            if (!quiet) {
                logger.info("'{}' Not Modified, skipping.", to)
            }
            return  //What we've got is already fine
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

        //Set the modify time to match the server's (if we know it)
        if (modifyTime > 0) {
            to.setLastModified(modifyTime)
        }

        //Save the ETag (if we know it)
        val eTag = connection.getHeaderField("ETag")
        if (eTag != null) {
            //Log if we get a weak ETag and we're not on quiet
            if (!quiet && eTag.startsWith("W/")) {
                logger.warn("Weak ETag found.")
            }
            saveETag(to, eTag, logger)
        }
    }

    /**
     * Creates a new file in the same directory as the given file with `.etag` on the end of the name.
     *
     * @param file The file to produce the ETag for
     * @return The (uncreated) ETag file for the given file
     */
    private fun getETagFile(file: File): File {
        return File(file.absoluteFile.parentFile, file.name + ".etag")
    }

    /**
     * Attempt to load an ETag for the given file, if it exists.
     *
     * @param to The file to load an ETag for
     * @param logger The logger to print errors to if it goes wrong
     * @return The ETag for the given file, or `null` if it doesn't exist
     */
    private fun loadETag(to: File, logger: Logger): String? {
        val eTagFile = getETagFile(to)
        return if (!eTagFile.exists()) {
            null
        } else try {
            eTagFile.readText()
        } catch (e: IOException) {
            logger.warn("Error reading ETag file '{}'.", eTagFile)
            null
        }
    }

    /**
     * Saves the given ETag for the given file, replacing it if it already exists.
     *
     * @param to The file to save the ETag for
     * @param eTag The ETag to be saved
     * @param logger The logger to print errors to if it goes wrong
     */
    private fun saveETag(to: File, eTag: String, logger: Logger) {
        val eTagFile = getETagFile(to)
        try {
            if (!eTagFile.exists()) {
                eTagFile.createNewFile()
            }
            eTagFile.writeText(eTag)
        } catch (e: IOException) {
            logger.warn("Error saving ETag file '{}'.", eTagFile, e)
        }
    }

    /**
     * Format the given number of bytes as a more human readable string.
     *
     * @param bytes The number of bytes
     * @return The given number of bytes formatted to kilobytes, megabytes or gigabytes if appropriate
     */
	@JvmStatic
	fun toNiceSize(bytes: Long): String {
        return if (bytes < 1024) {
            "$bytes B"
        } else if (bytes < 1024 * 1024) {
            (bytes / 1024).toString() + " KB"
        } else if (bytes < 1024 * 1024 * 1024) {
            String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        } else {
            String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Delete the file along with the corresponding ETag, if it exists.
     *
     * @param file The file to delete.
     */
    fun delete(file: File) {
        if (file.exists()) {
            file.delete()
        }
        val etagFile = getETagFile(file)
        if (etagFile.exists()) {
            etagFile.delete()
        }
    }
}