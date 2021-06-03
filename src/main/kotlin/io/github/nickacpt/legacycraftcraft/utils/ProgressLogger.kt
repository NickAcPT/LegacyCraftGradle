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
import java.util.HashSet
import java.util.LinkedHashMap
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta.Downloadable
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta.Downloads
import io.github.nickacpt.legacycraftcraft.providers.minecraft.MinecraftVersionMeta.OS
import org.gradle.api.Project
import java.lang.Exception
import java.lang.reflect.Method

/**
 * Wrapper to ProgressLogger internal API.
 */
class ProgressLogger private constructor(private val logger: Any?) {
    private val getDescription: Method?
    private val setDescription: Method?
    private val getShortDescription: Method?
    private val setShortDescription: Method?
    private val getLoggingHeader: Method?
    private val setLoggingHeader: Method?
    private val start: Method?
    private val started: Method?
    private val startedArg: Method?
    private val progress: Method?
    private val completed: Method?
    private val completedArg: Method?
    private fun getMethod(methodName: String, vararg args: Class<*>): Method? {
        if (logger != null) {
            try {
                return logger.javaClass.getMethod(methodName, *args)
            } catch (ignored: NoSuchMethodException) {
                // Nope
            }
        }
        return null
    }

    private operator fun invoke(method: Method?, vararg args: Any): Any? {
        if (logger != null) {
            try {
                method!!.isAccessible = true
                return method.invoke(logger, *args)
            } catch (ignored: IllegalAccessException) {
                // Nope
            } catch (ignored: InvocationTargetException) {
            }
        }
        return null
    }

    /**
     * Returns the description of the operation.
     *
     * @return the description, must not be empty.
     */
    val description: String?
        get() = invoke(getDescription) as String?

    /**
     * Sets the description of the operation. This should be a full, stand-alone description of the operation.
     *
     *
     * This must be called before [.started]
     *
     * @param description The description.
     */
    fun setDescription(description: String?): ProgressLogger {
        invoke(setDescription, description!!)
        return this
    }

    /**
     * Returns the short description of the operation. This is used in place of the full description when display space is limited.
     *
     * @return The short description, must not be empty.
     */
    val shortDescription: String?
        get() = invoke(getShortDescription) as String?

    /**
     * Sets the short description of the operation. This is used in place of the full description when display space is limited.
     *
     *
     * This must be called before [.started]
     *
     * @param description The short description.
     */
    fun setShortDescription(description: String?): ProgressLogger {
        invoke(setShortDescription, description!!)
        return this
    }

    /**
     * Returns the logging header for the operation. This is logged before any other log messages for this operation are logged. It is usually
     * also logged at the end of the operation, along with the final status message. Defaults to null.
     *
     *
     * If not specified, no logging header is logged.
     *
     * @return The logging header, possibly empty.
     */
    val loggingHeader: String?
        get() = invoke(getLoggingHeader) as String?

    /**
     * Sets the logging header for the operation. This is logged before any other log messages for this operation are logged. It is usually
     * also logged at the end of the operation, along with the final status message. Defaults to null.
     *
     * @param header The header. May be empty or null.
     */
    fun setLoggingHeader(header: String?): ProgressLogger {
        invoke(setLoggingHeader, header!!)
        return this
    }

    /**
     * Convenience method that sets descriptions and logs started() event.
     *
     * @return this logger instance
     */
    fun start(description: String?, shortDescription: String?): ProgressLogger {
        invoke(start, description!!, shortDescription!!)
        return this
    }

    /**
     * Logs the start of the operation, with no initial status.
     */
    fun started() {
        invoke(started)
    }

    /**
     * Logs the start of the operation, with the given status.
     *
     * @param status The initial status message. Can be null or empty.
     */
    fun started(status: String?) {
        invoke(started, status!!)
    }

    /**
     * Logs some progress, indicated by a new status.
     *
     * @param status The new status message. Can be null or empty.
     */
    fun progress(status: String?) {
        invoke(progress, status!!)
    }

    /**
     * Logs the completion of the operation, with no final status.
     */
    fun completed() {
        invoke(completed)
    }

    /**
     * Logs the completion of the operation, with a final status. This is generally logged along with the description.
     *
     * @param status The final status message. Can be null or empty.
     */
    fun completed(status: String?) {
        invoke(completed, status!!)
    }

    companion object {
        // Unsupported Gradle version// prior to Gradle 2.14
        // Gradle 2.14 and higher
        private val factoryClass: Class<*>?
            private get() {
                var progressLoggerFactoryClass: Class<*>? = null
                try {
                    // Gradle 2.14 and higher
                    progressLoggerFactoryClass =
                        Class.forName("org.gradle.internal.logging.progress.ProgressLoggerFactory")
                } catch (e: ClassNotFoundException) {
                    // prior to Gradle 2.14
                    try {
                        progressLoggerFactoryClass = Class.forName("org.gradle.logging.ProgressLoggerFactory")
                    } catch (ignored: ClassNotFoundException) {
                        // Unsupported Gradle version
                    }
                }
                return progressLoggerFactoryClass
            }

        /**
         * Get a Progress logger from the Gradle internal API.
         *
         * @param project The project
         * @param category The logger category
         * @return In any case a progress logger
         */
        fun getProgressFactory(project: Project, category: String?): ProgressLogger {
            return try {
                val getServices = project.javaClass.getMethod("getServices")
                val serviceFactory = getServices.invoke(project)
                val get = serviceFactory.javaClass.getMethod("get", Class::class.java)
                val progressLoggerFactory = get.invoke(serviceFactory, factoryClass)
                val newOperation = progressLoggerFactory.javaClass.getMethod("newOperation", String::class.java)
                ProgressLogger(newOperation.invoke(progressLoggerFactory, category))
            } catch (e: Exception) {
                project.logger.error("Unable to get progress logger. Download progress will not be displayed.")
                ProgressLogger(null)
            }
        }
    }

    init {
        getDescription = getMethod("getDescription")
        setDescription = getMethod("setDescription", String::class.java)
        getShortDescription = getMethod("getShortDescription")
        setShortDescription = getMethod("setShortDescription", String::class.java)
        getLoggingHeader = getMethod("getLoggingHeader")
        setLoggingHeader = getMethod("setLoggingHeader", String::class.java)
        start = getMethod("start", String::class.java, String::class.java)
        started = getMethod("started")
        startedArg = getMethod("started", String::class.java)
        progress = getMethod("progress", String::class.java)
        completed = getMethod("completed")
        completedArg = getMethod("completed", String::class.java)
    }
}