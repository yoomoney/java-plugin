package ru.yoomoney.gradle.plugins.backend.build

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.junit.Before
import java.io.File

abstract class AbstractPluginTest {

    val projectDir = TemporaryFolder()
    var originRepoFolder = TemporaryFolder()

    lateinit var buildFile: File
    lateinit var git: Git
    lateinit var gitOrigin: Git
    lateinit var gradleProperties: File

    @Before
    fun setup() {
        projectDir.create()
        originRepoFolder.create()

        buildFile = projectDir.newFile("build.gradle")

        buildFile.writeText("""
            buildscript {
                repositories {
                        jcenter()
                        mavenCentral()
                }
            }
            plugins {
                id 'ru.yoomoney.gradle.plugins.java-plugin'
            }
            dependencies {
                optional 'org.testng:testng:6.14.3'
            }
            javaModule.repositories = ["https://jcenter.bintray.com/",
                        "https://repo1.maven.org/maven2/"]
        """.trimIndent())

        projectDir.newFolder("src", "main", "java", "sample")
        val appSource = projectDir.newFile("src/main/java/sample/HelloWorld.java")
        appSource.writeText("""
            package sample;
            @javax.annotation.Nonnull
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.trimIndent())

        git = Git.init().setDirectory(File(projectDir.root.absolutePath))
                .setBare(false)
                .call()

        projectDir.newFile(".gitignore")
                .writeBytes(this::class.java.getResourceAsStream("/gitignore")
                        .readBytes())

        gradleProperties = projectDir.newFile("gradle.properties")
        gradleProperties.writeText("version=1.0.1-SNAPSHOT")

        projectDir.newFile("CHANGELOG.md").writeText("CHANGELOG")

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
        git.checkout().setName("feature/BACKEND-2588_build_jar").setCreateBranch(true).call()

        projectDir.newFile("README.md").writeText("README")
        git.add().addFilepattern(".").call()
        git.commit().setMessage("README commit").call()

        println("Work directory: ${projectDir.root.absolutePath}")
        println("Origin git repo directory: ${originRepoFolder.root.absolutePath}")
    }

    fun runTasksSuccessfully(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList() + "--stacktrace" + "-i")
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    fun runTasksOnJenkinsFail(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList() + "--stacktrace" + "-Pci=true")
                .withPluginClasspath()
                .forwardOutput()
                .buildAndFail()
    }

    fun runTasksFail(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList() + "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .buildAndFail()
    }

    fun assertFileExists(file: File) {
        assertTrue(file.exists())
    }

    fun assertFileAbsent(file: File) {
        assertFalse(file.exists())
    }

    fun writeSourceFile(path: String, fileName: String, source: String): File {
        projectDir.newFolder(*path.split("/").toTypedArray())
        val sourceFile = projectDir.newFile("$path/$fileName")
        sourceFile.writeText(source)
        return sourceFile
    }

    fun projectName(): String {
        return projectDir.root.name
    }
}