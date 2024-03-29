package ru.yoomoney.gradle.plugins.backend.build.sonarqube

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
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
        project.afterEvaluate {
            SonarqubeIncrementalAnalysisConfigurer.configure(project)
            resolveStaticProperties(project)
        }

        project.tasks.withType(SonarQubeTask::class.java).forEach { task ->
            task.onlyIf { isSonarqubeEnabled(project) }
            task.doFirst { resolveDynamicProperties(project) }

            project.tasks.withType(Checkstyle::class.java).forEach { task.dependsOn(it) }
            project.tasks.withType(JacocoReport::class.java).forEach { task.dependsOn(it) }
        }
    }

    private fun isSonarqubeEnabled(project: Project): Boolean {
        val javaModuleExtension = project.extensions.getByType(JavaExtension::class.java)
        if (javaModuleExtension.analyseDevelopmentBranchesOnly &&
            !GitManager(project).use { it.isDevelopmentBranch() }) {
            project.logger.warn("SonarQube is enabled on feature/ and hotfix/ and bugfix/ branches. Skipping.")
            return false
        }
        return javaModuleExtension.sonarqube.enabled
    }

    private fun resolveStaticProperties(project: Project) {
        val sonarqubeSettings = project.extensions.getByType(JavaExtension::class.java).sonarqube
        val sonarqubeExtension = project.extensions.getByType(SonarQubeExtension::class.java)
        val currentBranch = GitManager(project).use { it.branchName() }

        sonarqubeExtension.properties {
            if (sonarqubeSettings.projectKey != null) {
                it.properties.putIfAbsent("sonar.projectKey", sonarqubeSettings.projectKey)
            }
            if (sonarqubeSettings.projectName != null) {
                it.properties["sonar.projectName"] = sonarqubeSettings.projectName
            }
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

        if (project.logger.isDebugEnabled) {
            val sonarqubeTask = project.tasks.withType(SonarQubeTask::class.java).firstOrNull()
            sonarqubeTask?.properties?.forEach { (key, value) ->
                project.logger.debug("[sonarqube] properties: {}={}", key, value)
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
            .filter { it.reports.xml.required.get() }
            .mapNotNull { it.reports.xml?.outputLocation?.get()?.asFile }
            .filter { it.exists() }
            .map { it.absolutePath }
            .toList()
    }

    private fun resolveTestReportPaths(project: Project): List<String> {
        return project.tasks.withType(Test::class.java).asSequence()
            .mapNotNull { it.reports.junitXml?.outputLocation?.get()?.asFile }
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
            .flatMap { it.output }
            .filter { it.exists() }
            .mapNotNull { it.absolutePath }
            .toList()
    }

    private fun resolveTestSourceSets(project: Project): List<SourceSet> {
        val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        return listOfNotNull(
            javaPluginExtension.sourceSets.findByName(TestConfigurer.UNIT_TEST_SOURCE_SET_NAME),
            javaPluginExtension.sourceSets.findByName(TestConfigurer.COMPONENT_TEST_SOURCE_SET_NAME),
            javaPluginExtension.sourceSets.findByName(TestConfigurer.COMPONENT_TEST_DEPRECATED_SOURCE_SET_NAME)
        )
    }
}