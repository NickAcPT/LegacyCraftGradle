package io.github.nickacpt.legacycraftcraft.tasks

import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
import io.github.nickacpt.legacycraftcraft.mergeZip
import io.github.nickacpt.legacycraftcraft.resolveClasspathFile
import io.github.nickacpt.legacycraftcraft.utils.remapJar
import io.github.nickacpt.mixinofflineappliertool.MixinOfflineApplierTool
import io.github.nickacpt.mixinofflineappliertool.MixinSide
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
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

    @get:OutputDirectory
    val mixinsOutputDir: File
        get() = File(project.buildDir, "tmp" + File.separatorChar + "mixins-applied")

    @get:OutputFile
    val output: File
        get() = File(input.parentFile, input.nameWithoutExtension.removeSuffix("-dev") + "-jarmod.jar")

    @TaskAction
    fun doAction() {
        mixinsOutputDir.deleteRecursively()
        mixinsOutputDir.mkdirs()

        val tmpOutputMerge = File(output.parentFile, UUID.randomUUID().toString() + "-output.jar").also { ZipUtil.createEmpty(it) }
        val tmpOutputReobf = File(output.parentFile, UUID.randomUUID().toString() + "-reobf.jar").also { ZipUtil.createEmpty(it) }

        val minecraftMappedJar = project.legacyCraftExtension.minecraftProvider.minecraftMappedJar
        val minecraftUnMappedJar = project.legacyCraftExtension.minecraftProvider.minecraftJar
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

        val mappings =
            project.legacyCraftExtension.mappingsProvider.getMappingsForVersion(project.legacyCraftExtension.version)
                ?.reverse()

        println(":applyMixins - Reobfuscating")
        remapJar(project, tmpOutputMerge, tmpOutputReobf, mappings, resolveClassPath = true)

        minecraftUnMappedJar.copyTo(output, true)

        mergeZip(output, tmpOutputReobf)

        ZipUtil.removeEntry(output, "META-INF/")

        if (tmpOutputMerge.exists()) tmpOutputMerge.delete()
        if (tmpOutputReobf.exists()) tmpOutputReobf.delete()
        println(":applyMixins - Done")
    }

}
