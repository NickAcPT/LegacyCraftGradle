package io.github.nickacpt.legacycraftgradle.tasks

import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.mergeZip
import io.github.nickacpt.legacycraftgradle.resolveClasspathFile
import io.github.nickacpt.legacycraftgradle.utils.remapJar
import io.github.nickacpt.mixinofflineappliertool.MixinOfflineApplierTool
import io.github.nickacpt.mixinofflineappliertool.MixinSide
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withConvention
import org.zeroturnaround.zip.FileSource
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.util.*

open class ApplyMixinsTask : DefaultTask() {

    @get:InputFile
    lateinit var input: File

    private val mixinsOutputDir: File
        get() = File(project.buildDir, "tmp" + File.separatorChar + "mixins-applied")

    @get:OutputFile
    val output: File
        get() = File(input.parentFile, input.nameWithoutExtension.removeSuffix("-dev") + "-jarmod.jar")

    @get:OutputFile
    val outputDeobfuscated: File
        get() = File(input.parentFile, input.nameWithoutExtension.removeSuffix("-dev") + "-deobf.jar")

    @get:OutputFile
    val outputReobfuscated: File
        get() = File(input.parentFile, input.nameWithoutExtension.removeSuffix("-dev") + "-reobf.jar")

    @TaskAction
    fun doAction() {
        mixinsOutputDir.deleteRecursively()
        mixinsOutputDir.mkdirs()

        val tmpOutputMerge =
            File(output.parentFile, UUID.randomUUID().toString() + "-output.jar").also { ZipUtil.createEmpty(it) }

        val legacyCraftExtension = project.legacyCraftExtension
        val minecraftMappedJar = legacyCraftExtension.minecraftProvider.minecraftMappedJar
        val minecraftUnMappedJar = legacyCraftExtension.minecraftProvider.computeMinecraftJar(legacyCraftExtension)
        val sourceSets = project.withConvention(JavaPluginConvention::class) { sourceSets }
        val mixinFiles = sourceSets["main"]?.resources?.srcDirs?.first()?.listFiles()
            ?.filter { it.nameWithoutExtension.startsWith("mixins.") && it.name.endsWith(".json") }

        mixinFiles?.forEach {
            println(":applyMixins - Applying mixin configuration ${it.name}")
        }

        println(":applyMixins - Merging Jar with JarMod")
        mergeZip(tmpOutputMerge, input) { zipEntry ->
            val entryFileName = zipEntry.name.substringAfterLast("/")
            return@mergeZip !(entryFileName.startsWith("mixins.") && entryFileName.endsWith(".json"))
        }

        val modifiedEntries = MixinOfflineApplierTool.apply(
            minecraftMappedJar,
            input,
            mixinFiles?.map { it.name } ?: listOf(),
            MixinSide.CLIENT,
            mixinsOutputDir,
            project.resolveClasspathFile()
        )

        println(":applyMixins - Merging modified classes")
        ZipUtil.addOrReplaceEntries(tmpOutputMerge, *modifiedEntries.map {
            FileSource(it, File(mixinsOutputDir, it))
        }.toTypedArray())

        minecraftMappedJar.copyTo(outputDeobfuscated, true)
        mergeZip(outputDeobfuscated, tmpOutputMerge)

        val mappings =
            legacyCraftExtension.mappingsProvider.getMappingsForVersion().map { it.reverse() }

        println(":applyMixins - Reobfuscating")
        remapJar(project, tmpOutputMerge, outputReobfuscated, *mappings.toTypedArray(), resolveClassPath = true)

        minecraftUnMappedJar.copyTo(output, true)

        mergeZip(output, outputReobfuscated)

        ZipUtil.removeEntry(output, "META-INF/")

        if (tmpOutputMerge.exists()) tmpOutputMerge.delete()
        println(":applyMixins - Done")
    }

}
