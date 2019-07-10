package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.testng.annotations.Test
import java.io.File
import java.nio.file.Files
import java.util.Properties

/**
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 16.04.2019
 */
class JavaModulePluginTest : AbstractPluginTest() {

    @Test
    fun `should successfully run jar task`() {
        runTasksSuccessfully("build", "componentTest", "jar")
        assertFileExists(File(projectDir.root, "/target/libs/yamoney-${projectName()}-1.0.1-feature-BACKEND-2588-build-jar-SNAPSHOT.jar"))
        assertFileExists(File(projectDir.root, "/target/tmp/jar/MANIFEST.MF"))
        val properties = Properties().apply { load(File(projectDir.root, "/target/tmp/jar/MANIFEST.MF").inputStream()) }
        assertThat("Implementation-Version", properties.getProperty("Implementation-Version"), notNullValue())
        assertThat("Bundle-SymbolicName", properties.getProperty("Bundle-SymbolicName"), notNullValue())
        assertThat("Built-By", properties.getProperty("Built-By"), notNullValue())
        assertThat("Built-Date", properties.getProperty("Built-Date"), notNullValue())
        assertThat("Built-At", properties.getProperty("Built-At"), notNullValue())
    }

    @Test
    fun `should run tests`() {
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
        val buildResult = runTasksSuccessfully("test", "componentTest")
        assertThat("Java tests passed", buildResult.output, containsString("run java test..."))
        assertThat("Kotlin tests passed", buildResult.output, containsString("run kotlin test..."))
        assertThat("SlowTest tests passed", buildResult.output, containsString("run slowTest test..."))
        Files.delete(javaTest.toPath())
        Files.delete(kotlinTest.toPath())
        Files.delete(slowTest.toPath())
    }

    @Test
    fun `should skip spotBugs execution when findbugs limit is missing`() {
        val staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")
        staticAnalysisPropertiesFile.writeText("""
            dummy=123
        """.trimIndent())

        val buildResult = runTasksSuccessfully("checkFindBugsReport")

        assertThat(buildResult.output, containsString("Not found settings in static-analysis.properties for: type=findbugs"))
        assertThat(buildResult.output, containsString("findbugs limit not found, skipping check"))
        Files.delete(staticAnalysisPropertiesFile.toPath())
    }

    @Test
    fun `should find bug`() {
        val staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")
        staticAnalysisPropertiesFile.writeText("""
            findbugs=0
        """.trimIndent())
        val spotBugsSource = projectDir.newFile("src/main/java/sample/SpotBugs.java")
        spotBugsSource.writeText("""
            package sample;
            public class SpotBugs {
                private static void bcImpossibleCastWRONG() {
                    final Object doubleValue = Double.valueOf(1.0);
                    final Long value = (Long) doubleValue;
                    System.out.println("   - " + value);
                }
            }
        """.trimIndent())
        val buildResult = runTasksFail("test", "build")
        val reportTask = buildResult.tasks.find { it.path == ":checkFindBugsReport" }
        assertThat("Report task exists", reportTask, notNullValue())
        assertThat("Report task failed", reportTask?.outcome, equalTo(TaskOutcome.FAILED))
        assertThat(
                "Output contains SpotBugs failure",
                buildResult.output,
                containsString("Too much SpotBugs errors: actual=1, limit=0")
        )
        Files.delete(spotBugsSource.toPath())
        Files.delete(staticAnalysisPropertiesFile.toPath())
    }

    @Test
    fun `should pass spotbugs with 1 warning`() {
        val staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=5
            findbugs=1
        """.trimIndent())
        val spotBugsSource = projectDir.newFile("src/main/java/sample/SpotBugs.java")
        spotBugsSource.writeText("""
            package sample;
            public class SpotBugs {
                private static void bcImpossibleCastWRONG() {
                    final Object doubleValue = Double.valueOf(1.0);
                    final Long value = (Long) doubleValue;
                    System.out.println("   - " + value);
                }
            }
        """.trimIndent())
        val buildResult = runTasksSuccessfully("test", "build")
        val reportTask = buildResult.tasks.find { it.path == ":checkFindBugsReport" }
        assertThat("Report task exists", reportTask, notNullValue())
        assertThat("Report task succeed", reportTask?.outcome, equalTo(TaskOutcome.SUCCESS))
        assertThat(
                "Output contains SpotBugs pass",
                buildResult.output,
                containsString("SpotBugs successfully passed with 1 (limit=1) errors")
        )
        Files.delete(spotBugsSource.toPath())
        Files.delete(staticAnalysisPropertiesFile.toPath())
    }

    @Test
    fun `should only contain spotbugs task for main source set`() {
        val buildResult = runTasksSuccessfully("test", "build")
        val tasks = buildResult.tasks.filter { it.path.contains("spotbugs") }
        assertThat("Only one spotbugs task", tasks, hasSize(1))
    }
}
