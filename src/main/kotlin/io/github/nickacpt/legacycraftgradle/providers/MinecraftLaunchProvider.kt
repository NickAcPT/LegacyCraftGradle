package io.github.nickacpt.legacycraftgradle.providers

import io.github.nickacpt.legacycraftgradle.legacyCraftExtension
import org.gradle.api.Project

class MinecraftLaunchProvider(val project: Project) {

    fun provide() {
        project.legacyCraftExtension.abstraction.runProvider?.provideRun()
    }

}