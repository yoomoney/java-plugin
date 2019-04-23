package ru.yandex.money.gradle.plugins.backend.build.git

import org.gradle.api.Project

/**
 * TODO:
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 23.04.2019
 */
class GitFlowConfigurer {

    fun init(project: Project) {
        val gitManager = GitManager(project)
        project.extensions.extraProperties["branchName"] = gitManager.branchName()
        project.extensions.extraProperties["isMasterOrDev"] = gitManager.isMasterOrDev()
        project.extensions.extraProperties["isReleaseBranch"] = gitManager.isReleaseBranch()
        project.extensions.extraProperties["isFeatureBranch"] = gitManager.isFeatureBranch()
        if (gitManager.isFeatureBranch()) {
            val mainVersion = project.version.toString().removeSuffix("-SNAPSHOT")
            project.version = "$mainVersion-${gitManager.branchFullName()}-SNAPSHOT"
        }
    }

}