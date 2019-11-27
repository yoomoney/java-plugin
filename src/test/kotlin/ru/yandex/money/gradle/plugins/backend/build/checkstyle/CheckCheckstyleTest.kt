package ru.yandex.money.gradle.plugins.backend.build.checkstyle

import org.amshove.kluent.`should be equal to`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import ru.yandex.money.gradle.plugins.backend.build.AbstractPluginTest
import java.io.File
import java.io.FileInputStream
import java.util.Properties

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

        val buildResult = runTasksSuccessfully("checkCheckstyle")
        assertThat(buildResult.output, containsString("skipping check checkstyle"))
    }

    @Test
    fun `should skip task when checkStyleReport not found`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=3
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksSuccessfully("checkCheckstyle")
        assertThat(buildResult.output, containsString("Have not found"))
    }

    @Test
    fun `should return failed when too much checkstyle errors`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksFail("build")
        assertThat(buildResult.output, containsString("Too much checkstyle errors"))
    }

    @Test
    fun `should return failed when checkstyle limit is too high and ci build`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=10000
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksOnJenkinsFail("build")
        assertThat(buildResult.output, containsString("Сheckstyle limit is too high"))
        assertThat(buildResult.output, containsString("Decrease it in file static-analysis.properties."))
    }

    @Test
    fun `should override checkstyle limit if local build`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=10000
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksSuccessfully("build")
        assertThat(buildResult.output, containsString("Checkstyle check successfully passed"))

        val staticAnalysisLimits = Properties()
        staticAnalysisLimits.load(FileInputStream(staticAnalysisPropertiesFile))
        staticAnalysisLimits.getProperty("checkstyle") `should be equal to` "2"
    }

    @Test
    fun `should return success when checkstyle check successfully passed`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=2
            findbugs=0
        """.trimIndent())

        val buildResult = runTasksSuccessfully("build")
        assertThat(buildResult.output, containsString("Checkstyle check successfully passed"))
    }
}