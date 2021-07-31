package io.github.nickacpt.legacycraftgradle.tasks

import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import io.github.nickacpt.legacycraftgradle.mergeZip
import io.github.nickacpt.legacycraftgradle.resolveClasspathFile
import io.github.nickacpt.legacycraftgradle.utils.remapJar
import io.github.nickacpt.mixinofflineappliertool.MixinOfflineApplierTool
import io.github.nickacpt.mixinofflineappliertool.MixinSide
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.withConvention
import org.objectweb.asm.*
import org.zeroturnaround.zip.ByteSource
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.util.*
import java.util.zip.ZipEntry

open class ApplyMixinsTask : DefaultTask() {

    companion object {

        fun getMixinFiles(project: Project): List<File> {
            val sourceSets = project.withConvention(JavaPluginConvention::class) { sourceSets }
            val mixinFiles = sourceSets["main"]?.resources?.srcDirs?.first()?.listFiles()
                ?.filter { it.nameWithoutExtension.startsWith("mixins.") && it.name.endsWith(".json") }
            return mixinFiles ?: emptyList()
        }
    }

    @get:Internal
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
        val mixinFiles = getMixinFiles(project)

        var classAccessFixer: (ZipEntry, ByteArray) -> ByteArray = { _, bytes -> bytes }

        // When asked to run as deobfuscated, fix all class access
        if (project.legacyCraftExtension.runDeobfuscatedClient) {
            classAccessFixer = { entry, bytes -> fixClassAccess(entry, bytes) }
        }

        mixinFiles?.forEach {
            println(":applyMixins - Applying mixin configuration ${it.name}")
        }

        // Merge compiled jar (input from build) with a temporary empty zip file
        mergeZip(tmpOutputMerge, input, condition = { zipEntry ->
            val entryFileName = zipEntry.name.substringAfterLast("/")
            val isMixinConfiguration = entryFileName.startsWith("mixins.") && entryFileName.endsWith(".json")
            return@mergeZip !isMixinConfiguration
        }, byteMapper = classAccessFixer)

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
            ByteSource(it, fixClassAccess(null, File(mixinsOutputDir, it).readBytes()))
        }.toTypedArray())

        // Copy mapped (deobfuscated) minecraft to deobfuscated output jar
        if (project.legacyCraftExtension.buildDeobfuscatedJar) {
            minecraftMappedJar.copyTo(outputDeobfuscated, true)
            mergeZip(outputDeobfuscated, tmpOutputMerge, byteMapper = classAccessFixer)
        }

        // Load mappings and reverse them
        val mappings = legacyCraftExtension.mappingsProvider.getMappingsForVersion().reverse()

        // Apply mappings if needed to our temporary file.
        // At this moment it contains:
        //  - compiled jar (input from build)
        //  - mixin output classes (modified classes)
        if (!project.legacyCraftExtension.runDeobfuscatedClient) {
            println(":applyMixins - Reobfuscating")
            remapJar(project, tmpOutputMerge, outputReobfuscated, mappings, resolveClassPath = true)
        } else {
            println(":applyMixins - Skipping reobfuscation")
            // Running deobfuscated means that we don't obfuscate back at all
            // Create an empty zip and copy tmpOutputMerge to it.
            ZipUtil.createEmpty(outputReobfuscated)
            mergeZip(outputReobfuscated, tmpOutputMerge, byteMapper = classAccessFixer)
        }

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
            val targetJar =
                if (project.legacyCraftExtension.runDeobfuscatedClient) minecraftMappedJar else minecraftUnMappedJar

            targetJar.copyTo(output, true)

            mergeZip(output, outputReobfuscated, byteMapper = classAccessFixer)
            ZipUtil.removeEntry(output, "META-INF/")
        }

        if (!project.legacyCraftExtension.buildJarMod) output.delete()
        if (!project.legacyCraftExtension.buildDeobfuscatedJar) outputDeobfuscated.delete()
        if (tmpOutputMerge.exists()) tmpOutputMerge.delete()
        println(":applyMixins - Done")
    }

    private fun fixClassAccess(zipEntry: ZipEntry?, bytes: ByteArray): ByteArray {
        if (zipEntry == null || zipEntry.name.endsWith(".class")) {
            val reader = ClassReader(bytes)
            val writer = ClassWriter(0)

            reader.accept(object : ClassVisitor(Opcodes.ASM9, writer) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String?,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<String>?
                ) {
                    val accessVar =
                        access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).inv() or Opcodes.ACC_PUBLIC
                    super.visit(version, accessVar, name, signature, superName, interfaces)
                }

                override fun visitField(
                    access: Int,
                    name: String?,
                    descriptor: String?,
                    signature: String?,
                    value: Any?
                ): FieldVisitor {
                    val accessVar =
                        access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).inv() or Opcodes.ACC_PUBLIC
                    return super.visitField(accessVar, name, descriptor, signature, value)
                }

                override fun visitMethod(
                    access: Int,
                    name: String?,
                    descriptor: String?,
                    signature: String?,
                    exceptions: Array<String>?
                ): MethodVisitor {
                    val accessVar =
                        access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).inv() or Opcodes.ACC_PUBLIC
                    return super.visitMethod(accessVar, name, descriptor, signature, exceptions)
                }
            }, 0)

            writer.toByteArray()
        }

        return bytes
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
