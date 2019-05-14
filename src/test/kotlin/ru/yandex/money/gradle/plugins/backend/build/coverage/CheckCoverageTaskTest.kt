package ru.yandex.money.gradle.plugins.backend.build.coverage

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import ru.yandex.money.gradle.plugins.backend.build.AbstractPluginTest
import java.io.File
import java.nio.file.Files

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
        projectDir.newFolder("src", "test", "java")
        val javaTest = projectDir.newFile("src/test/java/JavaTest.java")
        javaTest.writeText("""
            import org.testng.annotations.Test;
            public class JavaTest {
                @Test
                public void javaTest() {
                    System.out.println("run java test...");
                }
            }
        """.trimIndent())

        projectDir.newFolder("src", "test", "kotlin")
        val kotlinTest = projectDir.newFile("src/test/kotlin/KotlinTest.kt")
        kotlinTest.writeText("""
            import org.testng.annotations.Test
            class KotlinTest {
                @Test
                fun `kotlin test`() {
                    println("run kotlin test...")
                }
            }
        """.trimIndent())

        projectDir.newFolder("src", "slowTest", "java")
        val slowTest = projectDir.newFile("src/slowTest/java/SlowTest.java")
        slowTest.writeText("""
            import org.testng.Assert;
            import org.testng.annotations.Test;
            public class SlowTest {
                @Test
                public void slowTest() throws Exception {
                    sample.HelloWorld.main(null);
                    System.out.println(ru.yandex.money.common.command.result.CommandResult.Status.SUCCESS);
                    System.out.println("run slowTest test...");
                }
            }
        """.trimIndent())
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
        Files.delete(javaTest.toPath())
        Files.delete(kotlinTest.toPath())
        Files.delete(slowTest.toPath())
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