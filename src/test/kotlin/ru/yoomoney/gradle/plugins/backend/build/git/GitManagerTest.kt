package ru.yoomoney.gradle.plugins.backend.build.git

import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevObject
import org.gradle.api.Project
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import ru.yoomoney.gradle.plugins.backend.build.AbstractPluginTest

/**
 * @author Petr Zinin pgzinin@yoomoney.ru
 * @since 22.02.2022
 */
class GitManagerTest : AbstractPluginTest() {

    @Test
    fun `should find remote branch`() {
        val repositoryDir = projectDir.root
        val project = mock(Project::class.java).also { doReturn(repositoryDir).`when`(it).projectDir }
        val manager = GitManager(project)

        gitOrigin.checkout().setName("origin-dev").setCreateBranch(true).call()
        gitOrigin.checkout().setName("origin-master").setCreateBranch(true).call()
        createBranch("origin-dev")
        createBranch("origin-master")

        git.fetch().setRemote("origin").call()

        assertThat(manager.findRemoteBranch("origin-dev"), equalTo("refs/remotes/origin/origin-dev"))
        assertThat(manager.findRemoteBranch("origin-master"), equalTo("refs/remotes/origin/origin-master"))
        assertThat(manager.findRemoteBranch("unknown"), nullValue())
    }

    @Test
    fun `should findCommonAncestor`() {
        val repositoryDir = projectDir.root
        val project = mock(Project::class.java).also { doReturn(repositoryDir).`when`(it).projectDir }
        val manager = GitManager(project)

        createBranch("dev")
        val commit = git.repository.resolve("HEAD").name

        createBranch("branch-0")
        createBranch("branch-1")
        commit("branch-1 commit")

        git.checkout().setName("branch-0").call()
        git.merge()
            .include(git.repository.resolve("branch-1"))
            .setFastForward(MergeCommand.FastForwardMode.NO_FF)
            .call()

        repeat(3) { commit("branch-0 commit #$it") }

        createBranch("branch-2")
        repeat(7) { commit("branch-2 commit #$it") }

        git.checkout().setName("dev").call()
        commit("dev commit")

        assertThat(manager.findCommonAncestor("dev", "branch-2"), equalTo(commit))
        assertThat(manager.findCommonAncestor("branch-2", "dev"), equalTo(commit))
        assertThat(manager.findCommonAncestor("refs/remotes/origin/master", "dev"),
            equalTo(git.repository.resolve("refs/remotes/origin/master").name()))
        assertThat(manager.findCommonAncestor("dev", "dev"),
            equalTo(git.repository.resolve("dev").name()))
    }

    @Test
    fun `should get commits count`() {
        // given
        val repositoryDir = projectDir.root
        val project = mock(Project::class.java).also { doReturn(repositoryDir).`when`(it).projectDir }
        val manager = GitManager(project)

        // when
        createBranch("dev")

        createBranch("branch-0")
        createBranch("branch-1")
        commit("branch-1 commit")

        git.checkout().setName("branch-0").call()
        git.merge()
            .include(git.repository.resolve("branch-1"))
            .setFastForward(MergeCommand.FastForwardMode.NO_FF)
            .call()

        repeat(3) { commit("branch-0 commit #$it") }

        createBranch("branch-2")
        repeat(7) { commit("branch-2 commit #$it") }

        // then
        assertThat(manager.getCommitCount("branch-0", "dev"), equalTo(5))
        assertThat(manager.getCommitCount("branch-2", "dev"), equalTo(12))
        assertThat(manager.getCommitCount("branch-2", "branch-0"), equalTo(7))
    }

    fun createBranch(branchName: String): Ref = git.checkout().setName(branchName).setCreateBranch(true).call()

    @Test
    fun `should find modified files`() {
        // given
        val repositoryDir = projectDir.root
        val project = mock(Project::class.java).also { doReturn(repositoryDir).`when`(it).projectDir }
        val manager = GitManager(project)

        repositoryDir.resolve("stable.txt").also { it.writeText("stable") }
        val forRemove = repositoryDir.resolve("for-remove.txt").also { it.writeText("for-remove") }
        val forUpdate = repositoryDir.resolve("for-update.txt").also { it.writeText("for-update") }
        val forMove = repositoryDir.resolve("for-move.txt").also { it.writeText("for-move") }
        val baseline = commit("base")

        // when
        val created = repositoryDir.resolve("new-file.txt")
        created.writeText("new-file")
        commit("add file")

        forRemove.delete()
        commit("delete file")

        forUpdate.appendText("updated")
        commit("update file")

        val moved = repositoryDir.resolve("renamed.txt")
        forMove.renameTo(moved)
        val head = commit("rename file")

        // then
        assertThat(
            manager.findModifiedFiles(head.name(), baseline.name()),
            containsInAnyOrder(forUpdate, created, moved)
        )
    }

    private fun commit(message: String): RevObject {
        git.add().addFilepattern(".").call()
        return git.commit().setMessage(message).call()
    }
}