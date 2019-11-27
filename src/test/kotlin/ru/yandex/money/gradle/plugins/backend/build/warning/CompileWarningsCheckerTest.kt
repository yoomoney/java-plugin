package ru.yandex.money.gradle.plugins.backend.build.warning

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.shouldEqual
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import ru.yandex.money.gradle.plugins.backend.build.AbstractPluginTest
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Тесты для [CompileWarningsChecker]
 *
 * @author Andrey Mochalov
 * @since 16.04.2019
 */
class CompileWarningsCheckerTest : AbstractPluginTest() {

    private lateinit var staticAnalysisPropertiesFile: File

    @BeforeMethod
    fun beforeMethod() {
        staticAnalysisPropertiesFile = projectDir.newFile("static-analysis.properties")

        projectDir.newFolder("src", "main", "java", "warning")
        val appSource = projectDir.newFile("src/main/java/warning/HelloWorld.java")
        appSource.writeText("""
            package warning;

            import java.lang.reflect.InvocationTargetException;
            import java.lang.reflect.Method;

            public class HelloWorld {

                public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
                    Class charsetClass = Class.forName("java.nio.charset.Charset");
                    Method newEncoder = charsetClass.getMethod("newEncoder", null);
                    Object encoder = newEncoder.invoke(new Object(), null);
                }

            }
        """.trimIndent())
    }

    @Test
    fun `should return success when count compile warning equals compile limit`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=3
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments("compileJava")
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()

        assertThat(TaskOutcome.SUCCESS, equalTo(result.task(":compileJava")?.outcome))
    }

    @Test
    fun `should return failed when compiler warnings limit is too high and ci build`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=10
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val result = runTasksOnJenkinsFail("compileJava")
        TaskOutcome.FAILED `should equal` result.task(":compileJava")?.outcome
    }

    @Test
    fun `should override compiler warnings limit if local build`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=10
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val result = runTasksSuccessfully("compileJava")
        TaskOutcome.SUCCESS shouldEqual result.task(":compileJava")?.outcome

        val staticAnalysisLimits = Properties()
        staticAnalysisLimits.load(FileInputStream(staticAnalysisPropertiesFile))
        staticAnalysisLimits.getProperty("compiler") `should be equal to` "3"
    }

    @Test
    fun `should return failed when too much compiler warnings`() {
        staticAnalysisPropertiesFile.writeText("""
            compiler=0
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments("compileJava")
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .buildAndFail()

        assertThat(TaskOutcome.FAILED, equalTo(result.task(":compileJava")?.outcome))
    }

    @Test
    fun `should skip compile check and return success when static-analysis or limit not found`() {
        staticAnalysisPropertiesFile.writeText("""
            checkstyle=0
            findbugs=0
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments("compileJava")
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()

        assertThat(TaskOutcome.SUCCESS, equalTo(result.task(":compileJava")?.outcome))
    }
}