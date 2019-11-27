package ru.yandex.money.gradle.plugins.backend.build.spotbugs

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.testng.annotations.Test
import ru.yandex.money.gradle.plugins.backend.build.AbstractPluginTest
import java.io.FileInputStream
import java.nio.file.Files
import java.util.Properties

class SpotBugsTest : AbstractPluginTest() {

    @Test
    fun `should skip spotBugs execution when findbugs limit is missing`() {
        val staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")
        staticAnalysisPropertiesFile.writeText("""
            dummy=123
        """.trimIndent())

        val buildResult = runTasksSuccessfully("checkFindBugsReport")

        MatcherAssert.assertThat(buildResult.output, CoreMatchers.containsString("findbugs limit not found, skipping check"))
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
        MatcherAssert.assertThat("Report task exists", reportTask, CoreMatchers.notNullValue())
        MatcherAssert.assertThat("Report task failed", reportTask?.outcome, CoreMatchers.equalTo(TaskOutcome.FAILED))
        MatcherAssert.assertThat(
                "Output contains SpotBugs failure",
                buildResult.output,
                CoreMatchers.containsString("Too much SpotBugs errors: actual=1, limit=0")
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
        MatcherAssert.assertThat("Report task exists", reportTask, CoreMatchers.notNullValue())
        MatcherAssert.assertThat("Report task succeed", reportTask?.outcome, CoreMatchers.equalTo(TaskOutcome.SUCCESS))
        MatcherAssert.assertThat(
                "Output contains SpotBugs pass",
                buildResult.output,
                CoreMatchers.containsString("SpotBugs successfully passed with 1 (limit=1) errors")
        )
        Files.delete(spotBugsSource.toPath())
        Files.delete(staticAnalysisPropertiesFile.toPath())
    }

    @Test
    fun `should override spotbugs limit if local build`() {
        val staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=5
            findbugs=20
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

        val buildResult = runTasksSuccessfully("build")
        buildResult.output shouldContain "SpotBugs successfully passed with 1 (limit=20) errors"

        val staticAnalysisLimits = Properties()
        staticAnalysisLimits.load(FileInputStream(staticAnalysisPropertiesFile))
        staticAnalysisLimits.getProperty("findbugs") `should be equal to` "1"

        Files.delete(spotBugsSource.toPath())
        Files.delete(staticAnalysisPropertiesFile.toPath())
    }

    @Test
    fun `should only contain spotbugs task for main source set`() {
        val buildResult = runTasksSuccessfully("test", "build")
        val tasks = buildResult.tasks.filter { it.path.contains("spotbugs") }
        MatcherAssert.assertThat("Only one spotbugs task", tasks, Matchers.hasSize(1))
    }
}