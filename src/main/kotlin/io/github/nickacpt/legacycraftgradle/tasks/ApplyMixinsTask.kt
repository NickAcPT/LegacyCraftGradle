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
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.withConvention
import org.zeroturnaround.zip.FileSource
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.util.*

open class ApplyMixinsTask : DefaultTask() {

    lateinit var inputFunc: () -> File

    @get:InputFile
    val input: File
        get() = inputFunc()

    private val mixinsOutputDir: File
        get() = File(project.buildDir, "tmp" + File.separatorChar + "mixins-applied")

    @get:OutputFile
    val output: File
        get() = File(input.parentFile, getInputName() + "-jarmod.jar")

    @get:OutputFile
    val outputDeobfuscated: File
        get() = File(input.parentFile, getInputName() + "-deobf.jar")

    @get:OutputFile
    val outputReobfuscated: File
        get() = File(input.parentFile, getInputName() + "-reobf.jar")

    @get:OutputFile
    val outputJavaAgent: File
        get() = File(input.parentFile, getInputName() + "-javaagent.jar")

    private fun getInputName() = input.nameWithoutExtension.removeSuffix("-dev")

    @TaskAction
    fun doAction() {
        deleteMixinsOutputDir()
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

        // Merge compiled jar (input from build) with a temporary empty zip file
        mergeZip(tmpOutputMerge, input) { zipEntry ->
            val entryFileName = zipEntry.name.substringAfterLast("/")
            val isMixinConfiguration = entryFileName.startsWith("mixins.") && entryFileName.endsWith(".json")
            return@mergeZip !isMixinConfiguration
        }

        // Apply classes and find them
        val modifiedEntries = MixinOfflineApplierTool.apply(
            minecraftMappedJar,
            input,
            mixinFiles?.map { it.name } ?: listOf(),
            MixinSide.CLIENT,
            mixinsOutputDir,
            project.resolveClasspathFile()
        )

        println(":applyMixins - Merging modified classes")

        // Merge mixin output classes (modified classes) with compiled jar (input from build)
        ZipUtil.addOrReplaceEntries(tmpOutputMerge, *modifiedEntries.map {
            FileSource(it, File(mixinsOutputDir, it))
        }.toTypedArray())

        // Copy mapped (deobfuscated) minecraft to deobfuscated output jar
        if (project.legacyCraftExtension.buildDeobfuscatedJar) {
            minecraftMappedJar.copyTo(outputDeobfuscated, true)
            mergeZip(outputDeobfuscated, tmpOutputMerge)
        }

        // Load mappings and reverse them
        val mappings = legacyCraftExtension.mappingsProvider.getMappingsForVersion().reverse()

        println(":applyMixins - Reobfuscating")
        // Apply mappings to our temporary file.
        // At this moment it contains:
        //  - compiled jar (input from build)
        //  - mixin output classes (modified classes)
        remapJar(project, tmpOutputMerge, outputReobfuscated, mappings, resolveClassPath = true)

        // Create JavaAgent jar if needeed
        if (project.legacyCraftExtension.buildJarModAgent) {
            println(":applyMixins - Creating Java Agent")
            createJavaAgent()
        } else {
            outputJavaAgent.delete()
        }

        // Create merged jarmod if requested by user
        if (project.legacyCraftExtension.buildJarMod) {
            println(":applyMixins - Merging Final Jar")
            minecraftUnMappedJar.copyTo(output, true)
            mergeZip(output, outputReobfuscated)

            ZipUtil.removeEntry(output, "META-INF/")
        }

        if (!project.legacyCraftExtension.buildJarMod) output.delete()
        if (!project.legacyCraftExtension.buildDeobfuscatedJar) outputDeobfuscated.delete()
        if (tmpOutputMerge.exists()) tmpOutputMerge.delete()
        println(":applyMixins - Done")
    }

    private fun deleteMixinsOutputDir() {
        mixinsOutputDir.deleteRecursively()
    }

    private fun createJavaAgent() {
        createEmptyJavaAgentJar(outputJavaAgent)
        mergeZip(outputJavaAgent, outputReobfuscated, "classes/")
    }

    private fun createEmptyJavaAgentJar(outputJavaAgent: File) {
        project.repositories.maven("https://jitpack.io") // Add Jitpack if needed
        val configuration = project.configurations.detachedConfiguration()
        configuration.dependencies.add(project.dependencies.create("com.github.NickAcPT:LegacyCraftAgent:259244584c"))

        configuration.resolve().first().copyTo(outputJavaAgent, true)
    }

}
