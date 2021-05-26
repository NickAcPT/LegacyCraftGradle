package io.github.nickacpt.legacycraftcraft.tasks

import io.github.nickacpt.legacycraftcraft.legacyCraftExtension
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

        val minecraftJar = project.legacyCraftExtension.minecraftProvider.minecraftMappedJar
        val sourceSets = project.withConvention(JavaPluginConvention::class) { sourceSets }
        val mixinFiles = sourceSets["main"]?.resources?.srcDirs?.first()?.listFiles()
            ?.filter { it.nameWithoutExtension.startsWith("mixins.") && it.name.endsWith(".json") }

        mixinFiles?.forEach {
            println(":applyMixins - Applying mixin configuration ${it.name}")
        }

        /*println(":applyMixins - Merging Jar with JarMod")
        mergeZip(output, input) { zipEntry ->
            val entryFileName = zipEntry.name.substringAfterLast("/")
            return@mergeZip !(entryFileName.startsWith("mixins.") && entryFileName.endsWith(".json"))
        }*/

        val modifiedEntries = MixinOfflineApplierTool.apply(
            minecraftJar,
            input,
            mixinFiles?.map { it.name } ?: listOf(),
            MixinSide.CLIENT,
            mixinsOutputDir,
            project.resolveClasspathFile()
        )

        minecraftJar.copyTo(output, true)
        ZipUtil.removeEntry(output, "META-INF/")

        /*println(":applyMixins - Merging modified classes")
        ZipUtil.replaceEntries(output, *modifiedEntries.map {
            FileSource(it, File(mixinsOutputDir, it))
        }.toTypedArray())*/
        val tmpOutput = File(output.parentFile, UUID.randomUUID().toString() + ".jar")

        output.renameTo(tmpOutput)

        val mappings =
            project.legacyCraftExtension.mappingsProvider.getMappingsForVersion(project.legacyCraftExtension.version)
                ?.reverse()

        println(":applyMixins - Reobfuscating")
        remapJar(project, tmpOutput, output, mappings, resolveClassPath = true)


        if (tmpOutput.exists()) tmpOutput.delete()
        println(":applyMixins - Done")
    }

}
