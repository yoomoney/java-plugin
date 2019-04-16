package ru.yandex.money.gradle.plugins.backend.build

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.hamcrest.MatcherAssert.assertThat
import org.junit.rules.TemporaryFolder
import org.testng.annotations.BeforeMethod
import ru.yandex.money.tools.testing.matcher.BooleanMatchers
import java.io.File
import java.nio.file.Paths

abstract class AbstractPluginTest {

    val projectDir = TemporaryFolder()
    var originRepoFolder = TemporaryFolder()

    lateinit var buildFile: File
    lateinit var git: Git
    lateinit var gitOrigin: Git
    lateinit var gradleProperties: File

    @BeforeMethod
    fun setup() {
        projectDir.create()
        originRepoFolder.create()

        buildFile = projectDir.newFile("build.gradle")

        buildFile.writeText("""
            buildscript {
                repositories {
                        maven { url 'https://nexus.yamoney.ru/content/repositories/thirdparty/' }
                        maven { url 'https://nexus.yamoney.ru/content/repositories/central/' }
                        maven { url 'https://nexus.yamoney.ru/content/repositories/releases/' }
                        maven { url 'https://nexus.yamoney.ru/content/repositories/jcenter.bintray.com/' }
                }
                dependencies {
                    classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.61'
                }
            }
            plugins {
                id 'java'
                id 'yamoney-java-module-plugin'
            }
        """.trimIndent())

        projectDir.newFolder("src", "main", "java", "sample")
        val appSource = projectDir.newFile("src/main/java/sample/HelloWorld.java")
        appSource.writeText("""
            package sample;
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.trimIndent())

        git = Git.init().setDirectory(File(projectDir.root.absolutePath))
                .setBare(false)
                .call()

        FileUtils.copyDirectory(Paths.get(System.getProperty("user.dir"), "tmp", "gradle-scripts").toFile(),
                Paths.get(projectDir.root.absolutePath, "tmp", "gradle-scripts").toFile())

        projectDir.newFile(".gitignore")
                .writeBytes(this::class.java.getResourceAsStream("/gitignore")
                        .readBytes())

        gradleProperties = projectDir.newFile("gradle.properties")
        gradleProperties.writeText("version=1.0.1-SNAPSHOT")

        projectDir.newFile("CHANGELOG.md").writeText("CHANGELOG")
        projectDir.newFile("README.md").writeText("README")

        git.add().addFilepattern(".").call()
        git.commit().setMessage("build.gradle commit").call()
        git.tag().setName("1.0.0").call()
        gitOrigin = Git.init().setDirectory(originRepoFolder.root)
                .setBare(true)
                .call()
        val remoteSetUrl = git.remoteSetUrl()
        remoteSetUrl.setRemoteUri(URIish("file://${originRepoFolder.root.absolutePath}/"))
        remoteSetUrl.setRemoteName("origin")
        remoteSetUrl.call()
        git.push()
                .setPushAll()
                .setPushTags()
                .call()
        git.checkout().setName("feature/BACKEND-2579_deb_build").setCreateBranch(true).call()

        println("Work directory: ${projectDir.root.absolutePath}")
        println("Origin git repo directory: ${originRepoFolder.root.absolutePath}")
    }

    fun runTasksSuccessfully(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList() + "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()
    }

    fun runTasksFail(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList() + "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .buildAndFail()
    }

    fun assertFileExists(file: File) {
        assertThat(file.absolutePath, file.exists(), BooleanMatchers.isTrue())
    }

    fun assertFileAbsent(file: File) {
        assertThat(file.absolutePath, file.exists(), BooleanMatchers.isFalse())
    }

    fun prepareTreeParser(objectId: ObjectId): AbstractTreeIterator {
        RevWalk(git.repository).use { walk ->
            val commit = walk.parseCommit(objectId)
            val tree = walk.parseTree(commit.tree.id)
            val treeParser = CanonicalTreeParser()
            git.repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }
            walk.dispose()
            return treeParser
        }
    }
}