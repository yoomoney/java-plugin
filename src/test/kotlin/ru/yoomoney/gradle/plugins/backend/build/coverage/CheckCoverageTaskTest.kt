package ru.yoomoney.gradle.plugins.backend.build.coverage

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import ru.yoomoney.gradle.plugins.backend.build.AbstractPluginTest
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.Properties

/**
 * Тесты для [CoverageConfigurer]
 *
 * @author Andrey Mochalov
 * @since 26.04.2019
 */
class CheckCoverageTaskTest : AbstractPluginTest() {

    private lateinit var coverageProperties: File

    @Before
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
        val buildResult = runTasksSuccessfully("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Have not found coverage.properties, skipping check.")
        )
    }

    @Test
    fun `should return failed when not found setting in coverage properties`() {
        coverageProperties.writeText("""
        """.trimIndent())
        val buildResult = runTasksFail("checkCoverage")
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
                    System.out.println("run slowTest test...");
                }
            }
        """.trimIndent())
        coverageProperties.writeText("""
            instruction=57.32
            branch=0
            method=50
            class=100
        """.trimIndent())
        val buildResult = runTasksSuccessfully("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage check successfully passed")
        )
        Files.delete(javaTest.toPath())
        Files.delete(kotlinTest.toPath())
        Files.delete(slowTest.toPath())
    }

    @Test
    fun `should return success and increase coverage when it's gone up on local machine`() {
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
                    System.out.println("run slowTest test...");
                }
            }
        """.trimIndent())
        coverageProperties.writeText("""
            instruction=57
            line=100
            branch=0
            method=40
            class=96
        """.trimIndent())

        val buildResult = runTasksSuccessfully("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage check successfully passed")
        )
        assertThat(
                buildResult.output,
                containsString("Coverage increased for type=method, setting limit to 50")
        )
        assertThat(
                buildResult.output,
                containsString("Coverage increased for type=class, setting limit to 100")
        )
        val coverageLimits = Properties().apply {
            FileInputStream(coverageProperties).use {
                load(it)
            }
        }
        coverageLimits.getProperty("method")
        assertThat(coverageLimits.getProperty("method"), equalTo("50"))
        Files.delete(javaTest.toPath())
        Files.delete(kotlinTest.toPath())
        Files.delete(slowTest.toPath())
    }

    @Test
    fun `should return failed and when coverage gone up on CI build`() {
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
                    System.out.println("run slowTest test...");
                }
            }
        """.trimIndent())
        coverageProperties.writeText("""
            instruction=57
            branch=0
            method=40
            class=100
        """.trimIndent())

        val buildResult = runTasksOnJenkinsFail("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage limit failure")
        )
        assertThat(
                buildResult.output,
                containsString("Great! Coverage gone up, increase it to 50 in coverage.properties and you're good to go: type=method, actual=50, limit=40")
        )
        val coverageLimits = Properties().apply {
            FileInputStream(coverageProperties).use {
                load(it)
            }
        }
        coverageLimits.getProperty("method")
        assertThat(coverageLimits.getProperty("method"), equalTo("40"))
        Files.delete(javaTest.toPath())
        Files.delete(kotlinTest.toPath())
        Files.delete(slowTest.toPath())
    }

    @Test
    fun `should return failed and rewrite only upped coverage properties if coverage gone down on local machine`() {
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
                    System.out.println("run slowTest test...");
                }
            }
        """.trimIndent())
        coverageProperties.writeText("""
            instruction=58
            branch=0
            method=40
            class=96
        """.trimIndent())

        val buildResult = runTasksFail("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage limit failure")
        )
        val coverageLimits = Properties().apply {
            FileInputStream(coverageProperties).use {
                load(it)
            }
        }
        coverageLimits.getProperty("method")
        assertThat(coverageLimits.getProperty("method"), equalTo("50"))
        assertThat(coverageLimits.getProperty("class"), equalTo("100"))
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
        val buildResult = runTasksFail("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Coverage limit failure")
        )
    }

    @Test
    fun `should return failed when dont' have limit for type`() {
        coverageProperties.writeText("""
        """.trimIndent())
        val buildResult = runTasksFail("checkCoverage")
        assertThat(
                buildResult.output,
                containsString("Not found settings in coverage.properties for: type=instruction")
        )
    }
}