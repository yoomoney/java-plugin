package ru.yandex.money.gradle.plugins.backend.build.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Project
import java.io.File

/**
 * Клиент для работы с гитом
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 23.04.2019
 */
class GitManager(project: Project) : AutoCloseable {

    private val git: Git = Git(
            FileRepositoryBuilder()
                    .setGitDir(File(project.projectDir, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build()
    )

    fun branchName(): String = git.repository.branch

    fun isMasterBranch(): Boolean = branchName().equals("master", true)

    fun isDevBranch(): Boolean = branchName().equals("dev", true)

    fun isMasterOrDev(): Boolean = isMasterBranch() || isDevBranch()

    fun isReleaseBranch(): Boolean = branchName().matches(Regex("(release)/.*"))

    fun isHotfixBranch(): Boolean = branchName().matches(Regex("(hotfix)/.*"))

    fun branchFullName() : String = branchName().replace(Regex("[^a-zA-Z0-9\\-\\.]+"), "-")

    fun isFeatureBranch(): Boolean {
        return when {
            isMasterOrDev() -> false
            isReleaseBranch() -> false
            isHotfixBranch() -> false
            else -> true
        }
    }

    override fun close() = git.close()

}