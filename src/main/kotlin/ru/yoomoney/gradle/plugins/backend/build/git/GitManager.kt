package ru.yoomoney.gradle.plugins.backend.build.git

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.gradle.api.Project
import java.io.File

/**
 * Клиент для работы с гитом
 *
 * @author Valerii Zhirnov
 * @since 23.04.2019
 */
class GitManager(private val project: Project) : AutoCloseable {

    private val git: Git = Git(
            FileRepositoryBuilder()
                    .readEnvironment()
                    .findGitDir(project.projectDir)
                    .build()
    )

    private fun describe(): String? = git.describe().setTags(true).call()

    private fun isMasterBranch(): Boolean = branchName().equals("master", true)

    private fun isDevBranch(): Boolean = branchName().equals("dev", true)

    private fun isMasterOrDev(): Boolean = isMasterBranch() || isDevBranch()

    private fun isReleaseBranch(): Boolean = branchName().matches(Regex("(release)/.*"))

    private fun isHotfixBranch(): Boolean = branchName().matches(Regex("(hotfix)/.*"))

    private fun isReleaseTag(): Boolean = describe()?.matches(Regex("\\d+\\.\\d+\\.\\d+")) ?: false

    private fun isStableBranch(): Boolean = isMasterOrDev() || isReleaseBranch() || isHotfixBranch() || isReleaseTag()

    fun isDevelopmentBranch(): Boolean = !isStableBranch()

    fun branchName(): String = git.repository.branch

    fun branchFullName(): String = branchName().replace(Regex("[^a-zA-Z0-9\\-\\.]+"), "-")

    /**
     * Поиск ветки во внешнем(`remote`) репозитории
     */
    fun findRemoteBranch(branchName: String): String? {
        return git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
            .firstOrNull { branch -> branchName.equals(git.repository.shortenRemoteBranchName(branch.name)) }
            ?.name
    }

    /**
     * Поиск ближайшего потомка (родителя) двух веток
     */
    fun findCommonAncestor(leftBranch: String, rightBranch: String): String {
        RevWalk(git.repository).use { revWalk ->
            revWalk.revFilter = RevFilter.MERGE_BASE
            revWalk.markStart(revWalk.parseCommit(git.repository.resolve(leftBranch)))
            revWalk.markStart(revWalk.parseCommit(git.repository.resolve(rightBranch)))
            return revWalk.next().name()
        }
    }

    /**
     * Получить количество коммитов расположенных в запрашиваемом промежутке git истории
     */
    fun getCommitCount(head: String, tail: String): Int {
        RevWalk(git.repository).use { revWalk ->
            return RevWalkUtils.count(
                revWalk,
                revWalk.parseCommit(git.repository.resolve(head)),
                revWalk.parseCommit(git.repository.resolve(tail))
            )
        }
    }

    /**
     * Поиск файлов измененных на выбранном промежутке git истории
     */
    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
    fun findModifiedFiles(head: String, tail: String): List<File> {
        git.repository.newObjectReader().use { reader ->
            val oldTree = CanonicalTreeParser()
            val newTree = CanonicalTreeParser()

            oldTree.reset(reader, git.repository.resolve("$tail^{tree}"))
            newTree.reset(reader, git.repository.resolve("$head^{tree}"))

            val diffs = git.diff()
                .setNewTree(newTree)
                .setOldTree(oldTree)
                .call()

            return diffs.map { project.projectDir.resolve(it.newPath) }
        }
    }

    override fun close() = git.close()
}
