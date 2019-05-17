package ru.yandex.money.gradle.plugins.backend.build.checkstyle

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import ru.yandex.money.gradle.plugins.backend.build.AbstractPluginTest
import java.io.File

/**
 * Тесты для [CheckCheckstyleTask]
 *
 * @author Andrey Mochalov
 * @since 30.04.2019
 */
class CheckCheckstyleTest : AbstractPluginTest() {

    private lateinit var staticAnalysisPropertiesFile: File

    @BeforeMethod
    fun beforeMethod() {
        staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")
    }

    @Test
    fun `should skip task when static-analysis properties not found or limit name not found`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=3
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksSuccessfully("clean", "checkCheckstyle")
        assertThat(buildResult.output, containsString("Not found settings in static-analysis.properties for: type=checkstyle"))
        assertThat(buildResult.output, containsString("skipping check checkstyle"))
    }

    @Test
    fun `should skip task when checkStyleReport not found`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=3
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksSuccessfully("clean", "checkCheckstyle")
        assertThat(buildResult.output, containsString("Have not found"))
    }

    @Test
    fun `should return failed when too much checkstyle errors`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksFail("clean", "build")
        assertThat(buildResult.output, containsString("Too much checkstyle errors"))
    }

    @Test
    fun `should return failed when checkstyle limit is too high`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=10000
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksFail("clean", "build")
        assertThat(buildResult.output, containsString("Сheckstyle limit is too high"))
        assertThat(buildResult.output, containsString("Decrease it in file static-analysis.properties."))
    }

    @Test
    fun `should return success when checkstyle check successfully passed`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=2
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksSuccessfully("clean", "build")
        assertThat(buildResult.output, containsString("Checkstyle check successfully passed"))
    }
}