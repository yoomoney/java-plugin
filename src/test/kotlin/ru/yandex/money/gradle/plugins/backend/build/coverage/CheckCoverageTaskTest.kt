package ru.yandex.money.gradle.plugins.backend.build.coverage

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import ru.yandex.money.gradle.plugins.backend.build.AbstractPluginTest
import java.io.File

/**
 * Тесты для [CoverageConfigurer]
 *
 * @author Andrey Mochalov
 * @since 26.04.2019
 */
class CheckCoverageTaskTest : AbstractPluginTest() {

    private lateinit var coverageProperties: File

    @BeforeMethod
    fun `before method`() {
        coverageProperties = projectDir.newFile("coverage.properties")
        coverageProperties.writeText("""
            instruction=11
            branch=12
            method=13
            class=14
        """.trimIndent())
    }

    @Test
    fun `should skip checkCoverage when coverage properties not found`() {
        coverageProperties.delete()
        val buildResult = runTasksSuccessfully("clean", "checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Have not found coverage.properties, skipping check.")
        )
    }

    @Test
    fun `should return failed when not found setting in coverage properties`() {
        coverageProperties.writeText("""
        """.trimIndent())
        val buildResult = runTasksFail("clean", "checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Not found settings in coverage.properties for: type=instruction")
        )
    }

    @Test
    fun `should return success when coverage check successfully passed`() {
        coverageProperties.writeText("""
            instruction=57
            branch=0
            method=50
            class=100
        """.trimIndent())
        val buildResult = runTasksSuccessfully("clean", "checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage check successfully passed")
        )
    }

    @Test
    fun `should return failed when coverage limit failure`() {
        coverageProperties.writeText("""
            instruction=100
            branch=100
            method=100
            class=100
        """.trimIndent())
        val buildResult = runTasksFail("clean", "checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage limit failure")
        )
    }
}