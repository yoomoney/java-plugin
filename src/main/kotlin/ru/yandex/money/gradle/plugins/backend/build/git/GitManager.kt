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

    private fun branchName(): String = git.repository.branch

    private fun isMasterBranch(): Boolean = branchName().equals("master", true)

    private fun isDevBranch(): Boolean = branchName().equals("dev", true)

    fun isMasterOrDev(): Boolean = isMasterBranch() || isDevBranch()

    fun branchFullName() : String = branchName().replace(Regex("[^a-zA-Z0-9\\-\\.]+"), "-")

    override fun close() = git.close()

}