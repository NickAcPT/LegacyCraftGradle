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
package io.github.nickacpt.legacycraftcraft.providers.minecraft

import com.fasterxml.jackson.databind.JsonNode
import io.github.nickacpt.legacycraftcraft.LegacyCraftPlugin.Companion.refreshDeps
import io.github.nickacpt.legacycraftcraft.utils.DownloadUtil.toNiceSize
import io.github.nickacpt.legacycraftcraft.utils.OperatingSystem.os
import java.io.IOException
import java.lang.RuntimeException
import java.lang.IllegalAccessException
import java.lang.reflect.InvocationTargetException
import java.lang.ClassNotFoundException
import java.lang.Runnable
import io.github.nickacpt.legacycraftcraft.LegacyCraftPlugin
import io.github.nickacpt.legacycraftcraft.utils.HashedDownloadUtil
import io.github.nickacpt.legacycraftcraft.utils.DownloadUtil
import java.io.FileNotFoundException
import io.github.nickacpt.legacycraftcraft.providers.minecraft.assets.AssetObject
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta.Downloadable
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta.Downloads
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta.OS
import java.io.File
import java.util.*

class MinecraftVersionMeta {
    val arguments: JsonNode? = null
    val assetIndex: AssetIndex? = null
    val assets: String? = null
    val complianceLevel = 0
    val downloads: Map<String, Download>? = null
    val id: String? = null
    val libraries: List<Library>? = null
    val logging: JsonNode? = null
    val mainClass: String? = null
    val minimumLauncherVersion = 0
    val releaseTime: String? = null
    val time: String? = null
    val type: String? = null
    fun getDownload(key: String): Download? {
        return downloads!![key]
    }

    class AssetIndex : Downloadable() {
        val id: String? = null
        val totalSize: Long = 0
        fun getFabricId(version: String): String {
            return if (id == version) version else "$version-$id"
        }
    }

    class Download : Downloadable {
        constructor(path: String?, sha1: String?, size: Long, url: String?) : super(path, sha1, size, url) {}
        constructor() {}

        fun relativeFile(baseDirectory: File?): File {
            return File(baseDirectory, path)
        }
    }

    class Library {
        val downloads: Downloads? = null
        val name: String? = null
        val natives: Map<String, String?>? = null
        val rules: List<Rule>? = null
        val isValidForOS: Boolean
            get() {
                if (rules == null || rules.isEmpty()) {
                    return true
                }
                for (rule in rules) {
                    if (rule.appliesToOS() && !rule.isAllowed) {
                        return false
                    }
                }
                return rules.stream().anyMatch { it: Rule -> it.isAllowed && it.oS == null }
            }

        fun hasNatives(): Boolean {
            return natives != null
        }

        fun hasNativesForOS(): Boolean {
            if (!hasNatives()) {
                return false
            }
            return if (natives!![os] == null) {
                false
            } else isValidForOS
        }

        val classifierForOS: Download?
            get() = downloads!!.getClassifier(natives!![os])
        val artifact: Artifact?
            get() = downloads?.artifact
    }

    class Downloads {
        val artifact: Artifact? = null
        val classifiers: Map<String?, Download>? = null
        fun getClassifier(os: String?): Download? {
            return classifiers!![os]
        }
    }

    class Artifact : Downloadable()
    class Classifier : Downloadable()
    class Rule {
        val action: String? = null
        val oS: OS? = null
        fun appliesToOS(): Boolean {
            return oS == null || oS.isValidForOS
        }

        val isAllowed: Boolean
            get() = action == "allow"
    }

    class OS {
        val name: String? = null
        val isValidForOS: Boolean
            get() = name == null || name.equals(os, ignoreCase = true)
    }

    // A base class for everything that can be downloaded
    abstract class Downloadable {
        constructor(path: String?, sha1: String?, size: Long, url: String?) {
            this.path = path
            this.sha1 = sha1
            this.size = size
            this.url = url
        }

        constructor() {}

        var path: String? = null
            private set
        var sha1: String? = null
            private set
        var size: Long = 0
            private set
        var url: String? = null
            private set

        fun getRelativeFile(baseDirectory: File?): File {
            Objects.requireNonNull(path, "Cannot get relative file from a null path")
            return File(baseDirectory, path)
        }
    }
}