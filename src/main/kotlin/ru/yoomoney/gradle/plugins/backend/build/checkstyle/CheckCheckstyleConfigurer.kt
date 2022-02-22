package ru.yoomoney.gradle.plugins.backend.build.checkstyle

import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.gradle.api.internal.file.TmpDirTemporaryFileProvider
import org.gradle.api.internal.resources.StringBackedTextResource
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension
import ru.yoomoney.gradle.plugins.backend.build.git.GitManager
import java.nio.charset.StandardCharsets

/**
 * Конфигурация checkstyle
 *
 * @author Andrey Mochalov
 * @since 30.04.2019
 */
class CheckCheckstyleConfigurer {

    companion object {
        private const val DEFAULT_CHECKSTYLE_CONFIG = "/checkstyle.xml"
    }

    fun init(project: Project) {
        configureCheckstyleExtensions(project)

        project.tasks.withType(Checkstyle::class.java) {
            it.reports { reports ->
                reports.xml.isEnabled = true
                reports.html.isEnabled = false
            }
        }

        val gitManager = GitManager(project)
        project.tasks.withType(Checkstyle::class.java).forEach { task ->
            task.onlyIf { checkstyleEnabled(task.project, gitManager) }
        }

        val checkCheckstyleTask = project.tasks.create("checkCheckstyle", CheckCheckstyleTask::class.java)

        project.tasks.getByName("checkstyleMain").finalizedBy(checkCheckstyleTask)
    }

    private fun checkstyleEnabled(project: Project, gitManager: GitManager): Boolean {
        val javaModuleExtension = project.extensions.getByType(JavaExtension::class.java)
        if (javaModuleExtension.analyseDevelopmentBranchesOnly && !gitManager.isDevelopmentBranch()) {
            project.logger.warn("Checkstyle is enabled on feature/ and hotfix/ and bugfix/ branches. Skipping.")
            return false
        }
        return javaModuleExtension.checkstyleEnabled
    }

    private fun configureCheckstyleExtensions(project: Project) {
        val checkstyleExtension = project.extensions.getByType(CheckstyleExtension::class.java)
        checkstyleExtension.toolVersion = "7.3"
        checkstyleExtension.sourceSets = listOf(project.convention.getPlugin(JavaPluginConvention::class.java)
                .sourceSets.getAt("main"))
        checkstyleExtension.isIgnoreFailures = true
        checkstyleExtension.reportsDir = project.file("${project.buildDir}/checkstyleReports")
        checkstyleExtension.config = StringBackedTextResource(TmpDirTemporaryFileProvider(), checkstyleConfig())
    }

    private fun checkstyleConfig(): String {
        val inputStream = this.javaClass.getResourceAsStream(DEFAULT_CHECKSTYLE_CONFIG)
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8)
    }
}
