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
package io.github.nickacpt.legacycraftgradle.utils

import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.gradle.api.logging.Logging
import java.io.File
import java.io.IOException

object Checksum {
    private val log = Logging.getLogger(Checksum::class.java)
    fun equals(file: File?, checksum: String): Boolean {
        if (file == null || !file.exists()) {
            return false
        }
        try {
            val hash = Files.asByteSource(file).hash(Hashing.sha1())
            log.debug("Checksum check: '$hash' == '$checksum'?")
            return hash.toString() == checksum
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun sha256(file: File): ByteArray {
        return try {
            val hash = Files.asByteSource(file).hash(Hashing.sha256())
            hash.asBytes()
        } catch (e: IOException) {
            throw RuntimeException("Failed to get file hash")
        }
    }
}