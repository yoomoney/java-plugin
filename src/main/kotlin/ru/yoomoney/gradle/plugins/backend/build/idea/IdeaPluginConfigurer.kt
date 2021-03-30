package ru.yoomoney.gradle.plugins.backend.build.idea

import org.gradle.api.Project
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel

/**
 * Настраивает ide
 *
 * @author Valerii Zhirnov
 * @since 17.04.2019
 */
class IdeaPluginConfigurer {

    fun init(target: Project) {
        val ideaPlugin = target.plugins.getPlugin(IdeaPlugin::class.java)
        val ideaModule = ideaPlugin.model.module
        ideaModule.scopes["PROVIDED"]?.plusAssign(
                mapOf("optional" to listOf(target.configurations.getByName("optional")))
        )
        ideaModule.isDownloadJavadoc = true
        ideaModule.isDownloadSources = true
        ideaModule.inheritOutputDirs = true

        target.extensions.configure(IdeaModel::class.java) {
            it.project.ipr.withXml { provider ->

                val node = provider.asNode()
                node.appendNode("ALEXALEX", "ALEXALEX")
            }
        }
//        ideaModule.jdkName = "12"

        ideaModule.excludeDirs.minusAssign(target.buildDir)
        val toExclude = listOf(
                "classes", "docs", "jacoco", "deb-templates", "publications", "out", "tmp",
                "dependency-cache", "resources", "libs", "test-results", "test-reports", "reports",
                "production", "test", "findbugsReports", "debSource", "debSourceDeploy", "debian",
                "distributions", "bindings-common", "schema", "checkstyleReports", "../build"
        )
        toExclude.forEach {
            ideaModule.excludeDirs.plusAssign(target.file("${target.buildDir}/$it"))
        }
    }
}
