package ru.yoomoney.gradle.plugins.backend.build.sonarqube

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarQubeExtension
import org.sonarqube.gradle.SonarQubeTask
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension
import ru.yoomoney.gradle.plugins.backend.build.checkstyle.CheckCheckstyleTask
import ru.yoomoney.gradle.plugins.backend.build.git.GitManager
import ru.yoomoney.gradle.plugins.backend.build.test.TestConfigurer

/**
 * Настраивает задачу запуска SonarScanner
 *
 * @author Petr Zinin pgzinin@yoomoney.ru
 * @since 11.02.2022
 */
@SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
class SonarqubeConfigurer {

    fun init(project: Project) {
        val sonarqubeSettings = project.extensions.getByType(JavaExtension::class.java).sonarqube

        resolveStaticProperties(project)

        project.tasks.withType(SonarQubeTask::class.java).forEach { task ->
            task.onlyIf { sonarqubeSettings.enabled }
            task.doFirst { resolveDynamicProperties(project) }

            project.tasks.withType(CheckCheckstyleTask::class.java).forEach { task.dependsOn(it) }
            project.tasks.withType(JacocoReport::class.java).forEach { task.dependsOn(it) }
        }
    }

    private fun resolveStaticProperties(project: Project) {
        val sonarqubeSettings = project.extensions.getByType(JavaExtension::class.java).sonarqube
        val sonarqubeExtension = project.extensions.getByType(SonarQubeExtension::class.java)
        val currentBranch = GitManager(project).branchName()

        sonarqubeExtension.properties {
            it.properties.putIfAbsent("sonar.projectKey", sonarqubeSettings.projectKey)
            it.properties.putIfAbsent("sonar.branch.name", currentBranch)

            if (!sonarqubeSettings.supplyLibrariesPath) {
                it.properties.put("sonar.java.libraries", "")
                it.properties.put("sonar.java.test.libraries", "")
            }
        }
    }

    private fun resolveDynamicProperties(project: Project) {
        project.allprojects.forEach { cursor ->
            val extension = cursor.extensions.getByType(SonarQubeExtension::class.java)
            extension.properties {
                it.properties["sonar.junit.reportPaths"] = resolveTestReportPaths(cursor)
                it.properties["sonar.coverage.jacoco.xmlReportPaths"] = resolveJacocoReportPaths(cursor)
                it.properties["sonar.java.checkstyle.reportPaths"] = resolveCheckstyleReportPaths(cursor)
                it.properties["sonar.tests"] = resolveTestSourcePaths(cursor)
                it.properties["sonar.java.test.binaries"] = resolveTestBinariesPaths(cursor)
            }
        }
    }

    private fun resolveCheckstyleReportPaths(project: Project): String? {
        return project.extensions.findByType(CheckstyleExtension::class.java)
            ?.reportsDir
            ?.resolve(CheckCheckstyleTask.CHECKSTYLE_REPORT_FILE_NAME)
            ?.takeIf { it.exists() }
            ?.absolutePath
    }

    private fun resolveJacocoReportPaths(project: Project): List<String> {
        return project.tasks.withType(JacocoReport::class.java).asSequence()
            .filter { it.reports.xml.isEnabled }
            .mapNotNull { it.reports.xml?.destination }
            .filter { it.exists() }
            .map { it.absolutePath }
            .toList()
    }

    private fun resolveTestReportPaths(project: Project): List<String> {
        return project.tasks.withType(Test::class.java).asSequence()
            .mapNotNull { it.reports.junitXml?.destination }
            .filter { it.exists() }
            .mapNotNull { it.absolutePath }
            .toList()
    }

    private fun resolveTestSourcePaths(project: Project): List<String> {
        return resolveTestSourceSets(project)
            .flatMap { it.allSource.sourceDirectories }
            .filter { it.exists() }
            .mapNotNull { it.absolutePath }
            .toList()
    }

    private fun resolveTestBinariesPaths(project: Project): List<String> {
        return resolveTestSourceSets(project)
            .flatMap { it.compileClasspath }
            .filter { it.exists() }
            .mapNotNull { it.absolutePath }
            .toList()
    }

    private fun resolveTestSourceSets(project: Project): List<SourceSet> {
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        return listOfNotNull(
            javaPluginConvention.sourceSets.findByName(TestConfigurer.UNIT_TEST_SOURCE_SET_NAME),
            javaPluginConvention.sourceSets.findByName(TestConfigurer.COMPONENT_TEST_SOURCE_SET_NAME),
            javaPluginConvention.sourceSets.findByName(TestConfigurer.COMPONENT_TEST_DEPRECATED_SOURCE_SET_NAME)
        )
    }
}