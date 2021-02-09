package ru.yoomoney.gradle.plugins.backend.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import ru.yoomoney.gradle.plugins.backend.build.AbstractPluginTest
import java.nio.file.Files

/**
 * @author Valerii Zhirnov
 * @since 13.05.2019
 */
class KotlinPluginTest : AbstractPluginTest() {

    @Before
    fun before() {
        buildFile.writeText("""
            buildscript {
                repositories {
                        jcenter()
                        mavenCentral()
                }
            }
            plugins {
                id 'ru.yoomoney.gradle.plugins.kotlin-plugin'
            }
            repositories {
                jcenter()
                mavenCentral()
            }
            dependencies {
                implementation 'org.testng:testng:6.14.3'
            }
        """.trimIndent())
    }

    @Test
    fun `should fail with detekt errors`() {
        val sourceFile = writeSourceFile("src/main/kotlin", "Sample.kt", "class InvalidClass {}")
        val staticAnalysis = projectDir.newFile("static-analysis.properties")
        staticAnalysis.writeText("""
            detekt=0
        """.trimIndent())
        val buildResult = runTasksFail("build", "check")
        val detektTask = buildResult.tasks.find { it.path.contains(":detekt") }
        MatcherAssert.assertThat("Detekt task exists", detektTask, CoreMatchers.notNullValue())
        MatcherAssert.assertThat("Detekt task failed", detektTask?.outcome, CoreMatchers.equalTo(TaskOutcome.FAILED))
        MatcherAssert.assertThat(
                "Output contains Detekt failure",
                buildResult.output,
                CoreMatchers.containsString("Build failed with 3 weighted issues")
        )
        Files.delete(sourceFile.toPath())
        Files.delete(staticAnalysis.toPath())
    }

    @Test
    fun `should not fail with detekt errors`() {
        val sourceFile = writeSourceFile("src/main/kotlin", "Sample.kt", "class InvalidClass {}")
        val staticAnalysis = projectDir.newFile("static-analysis.properties")
        staticAnalysis.writeText("""
            detekt=3
        """.trimIndent())
        val buildResult = runTasksFail("build", "check")
        println(buildResult.output)
        val detektTask = buildResult.tasks.find { it.path.contains(":detekt") }

        MatcherAssert.assertThat("Detekt task exists", detektTask, CoreMatchers.notNullValue())
        MatcherAssert.assertThat("Detekt task succeeded", detektTask?.outcome, CoreMatchers.equalTo(TaskOutcome.SUCCESS))
        Files.delete(sourceFile.toPath())
        Files.delete(staticAnalysis.toPath())
    }

    @Test
    fun `should fail with ktlint errors`() {
        val sourceFile = writeSourceFile(
                "src/main/kotlin",
                "Sample.kt",
                """
                fun main(args: Array<String>) {
                    (1..2).map { v -> "Value: " + v }.forEach { v -> println("Gradle " + v) };
                }
                """.trimIndent() + '\n'
        )
        val buildResult = runTasksFail("build", "check")
        val ktlintTask = buildResult.tasks.find { it.path.contains(":ktlintMainSourceSetCheck") }
        MatcherAssert.assertThat("ktlint task exists", ktlintTask, CoreMatchers.notNullValue())
        MatcherAssert.assertThat("ktlint task failed", ktlintTask?.outcome, CoreMatchers.equalTo(TaskOutcome.FAILED))
        MatcherAssert.assertThat(
                "Output contains ktlint failure",
                buildResult.output,
                CoreMatchers.containsString("Unnecessary semicolon")
        )
        Files.delete(sourceFile.toPath())
    }

    @Test
    fun `should pass all checks`() {
        val sourceFile = writeSourceFile(
                "src/main/kotlin",
                "Sample.kt",
                """
                fun main(args: Array<String>) {
                    (1..2).map { v -> "Value: " + v }.forEach { v -> println("Gradle " + v) }
                }
                """.trimIndent() + '\n'
        )
        runTasksSuccessfully("build", "check")
        Files.delete(sourceFile.toPath())
    }
}
