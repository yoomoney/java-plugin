package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
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
        runTasksSuccessfully("clean", "build", "slowTest", "jar")
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
        val buildResult = runTasksSuccessfully("clean", "test", "slowTest")
        assertThat("Java tests passed", buildResult.output, containsString("run java test..."))
        assertThat("Kotlin tests passed", buildResult.output, containsString("run kotlin test..."))
        assertThat("SlowTest tests passed", buildResult.output, containsString("run slowTest test..."))
    }

    @Test
    fun `should find bug`() {
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
        val buildResult = runTasksFail("clean", "test", "build")
        val reportTask = buildResult.tasks.find { it.path == ":checkFindBugsReport" }
        assertThat("Report task exists", reportTask, notNullValue())
        assertThat("Report task failed", reportTask?.outcome, equalTo(TaskOutcome.FAILED))
        assertThat(
                "Output contains SpotBugs failure",
                buildResult.output,
                containsString("Too much SpotBugs errors: actual=1, limit=0")
        )
        Files.delete(spotBugsSource.toPath())
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
        val buildResult = runTasksSuccessfully("clean", "test", "build")
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
}
