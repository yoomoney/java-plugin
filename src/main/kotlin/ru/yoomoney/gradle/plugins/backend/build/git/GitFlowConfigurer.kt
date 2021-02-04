package ru.yoomoney.gradle.plugins.backend.build.git

import org.gradle.api.Project

/**
 * Проставляет параметры из гита для release flow
 *
 * @author Valerii Zhirnov
 * @since 23.04.2019
 */
class GitFlowConfigurer {

    fun init(project: Project) {
        val gitManager = GitManager(project)
        if (gitManager.isDevelopmentBranch()) {
            val mainVersion = project.version.toString().removeSuffix("-SNAPSHOT")
            project.version = "$mainVersion-${gitManager.branchFullName()}-SNAPSHOT"
        }
    }
}
