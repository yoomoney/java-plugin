package ru.yoomoney.gradle.plugins.backend.build.sonarqube

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.sonarqube.gradle.SonarQubeExtension
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension
import ru.yoomoney.gradle.plugins.backend.build.git.GitManager
import ru.yoomoney.gradle.plugins.backend.build.test.TestConfigurer
import java.io.File

/**
 * Настройка инкрементального анализа в SonarQube
 *
 * @author Petr Zinin pgzinin@yoomoney.ru
 * @since 02.03.2022
 */
@SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
object SonarqubeIncrementalAnalysisConfigurer {

    fun configure(project: Project) {
        if (!isIncrementalAnalysisEnabled(project)) {
            project.logger.lifecycle("[sonarqube] incremental analysis disabled")
            return
        }

        val modifiedFiles = findModifiedFiles(project) ?: return
        val sonarqubeExtension = project.extensions.getByType(SonarQubeExtension::class.java)

        sonarqubeExtension.properties {
            it.properties["sonar.inclusions"] = resolveMainSourcePaths(project, modifiedFiles)
            it.properties["sonar.test.inclusions"] = resolveTestSourcePaths(project, modifiedFiles)
        }
        project.logger.lifecycle("[sonarqube] incremental analysis configured: modifiedFiles.count={}",
            modifiedFiles.size)
    }

    private fun isIncrementalAnalysisEnabled(project: Project): Boolean {
        val sonarqubeSettings = project.extensions.getByType(JavaExtension::class.java).sonarqube
        val currentBranch = GitManager(project).use { it.branchName() }
        return sonarqubeSettings.incrementalAnalysisEnabled &&
                sonarqubeSettings.stableBranches.isNotEmpty() &&
                !sonarqubeSettings.stableBranches.contains(currentBranch)
    }

    private fun findModifiedFiles(project: Project): List<File>? {
        val closestStableAncestor = resolveClosestStableAncestor(project) ?: return null
        project.logger.lifecycle("[sonarqube] found closest stable ancestor: commit={}", closestStableAncestor)

        GitManager(project).use { gitManager ->
            return gitManager.findModifiedFiles(head = gitManager.branchName(), tail = closestStableAncestor)
                .filter { it.exists() }
                .map { it.absoluteFile }
        }
    }

    private fun resolveClosestStableAncestor(project: Project): String? {
        val sonarqubeSettings = project.extensions.getByType(JavaExtension::class.java).sonarqube
        GitManager(project).use { gitManager ->
            val currentBranch = gitManager.branchName()
            return sonarqubeSettings.stableBranches
                .mapNotNull { gitManager.findRemoteBranch(it) }
                .map { gitManager.findCommonAncestor(it, currentBranch) }
                .map { it to gitManager.getCommitCount(head = currentBranch, tail = it) }
                .minBy { it.second }
                ?.first
        }
    }

    private fun resolveMainSourcePaths(project: Project, modifiedFiles: List<File>): Set<String> {
        return resolveSourceSetFilePaths(project, modifiedFiles, "main")
    }

    private fun resolveTestSourcePaths(project: Project, modifiedFiles: List<File>): Set<String> {
        return resolveSourceSetFilePaths(project, modifiedFiles, TestConfigurer.UNIT_TEST_SOURCE_SET_NAME,
            TestConfigurer.COMPONENT_TEST_SOURCE_SET_NAME, TestConfigurer.COMPONENT_TEST_DEPRECATED_SOURCE_SET_NAME)
    }

    @SuppressFBWarnings("PDP_POORLY_DEFINED_PARAMETER")
    private fun resolveSourceSetFilePaths(
        project: Project,
        modifiedFiles: List<File>,
        vararg sourceSet: String
    ): Set<String> {
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSetDirectories = sourceSet.toList()
            .mapNotNull { javaPluginConvention.sourceSets.findByName(it) }
            .flatMap { it.allJava.sourceDirectories }
            .toSet()

        return modifiedFiles
            .filter { modifiedFile -> sourceSetDirectories.any { sourceDir -> modifiedFile.startsWith(sourceDir) } }
            .map { it.relativeTo(project.projectDir).path }
            .toSet()
    }
}