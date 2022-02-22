package ru.yoomoney.gradle.plugins.backend.build

import org.apache.commons.io.IOUtils
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties

/**
 * @author Valerii Zhirnov
 * @since 16.04.2019
 */
class JavaModulePluginTest : AbstractPluginTest() {

    @Test
    fun `should successfully run jar task`() {
        buildFile.writeText("""
            buildscript {
                repositories {
                        mavenCentral()
                }
            }
            plugins {
                id 'ru.yoomoney.gradle.plugins.java-plugin'
            }
            dependencies {
                optional 'org.testng:testng:6.14.3'
            }
            javaModule {
                 repositories = ["https://repo1.maven.org/maven2/"]
            }
        """.trimIndent())

        runTasksSuccessfully("build", "jar")
        assertFileExists(File(projectDir.root, "/target/libs/${projectName()}-1.0.1-feature-BACKEND-2588-build-jar-SNAPSHOT.jar"))
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
                    System.out.println("run slowTest test...");
                }
            }
        """.trimIndent())

        projectDir.newFolder("target", "tmp", "logs", "test")
        val slowTestLogs = projectDir.newFile("target/tmp/logs/test/LOGS-SlowTest.xml")
        slowTestLogs.writeText("""
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <testlogs className="SlowTest">
                <testlog name="slowTest"><![CDATA[
                       [test] SUCCESS
                       [test] run slowTest test... 
                ]]></testlog>
            </testlogs>    
        """.trimIndent())

        val buildResult = runTasksSuccessfully("test", "componentTest")
        assertThat("Java tests passed", buildResult.output, containsString("run java test..."))
        assertThat("Kotlin tests passed", buildResult.output, containsString("run kotlin test..."))
        assertThat("SlowTest tests passed", buildResult.output, containsString("run slowTest test..."))
        assertThat("SlowTest reports overwritten",
                IOUtils.toString(
                        File(projectDir.root, "/target/test-results/slowTestTestNg/TEST-SlowTest.xml").inputStream(),
                        StandardCharsets.UTF_8
                ),
                containsString("[test]")
        )
        Files.delete(javaTest.toPath())
        Files.delete(kotlinTest.toPath())
        Files.delete(slowTest.toPath())
    }

    @Test
    fun `should pass parameters to sonarqube tasks`() {
        buildFile.appendText("\n")
        buildFile.appendText("""
            javaModule {
                sonarqube.projectKey = "projectKey"
                sonarqube.supplyLibrariesPath = false
            }
            
            task printSonarqubeProperties {
                doLast {
                    project.tasks.getByName("sonarqube").properties.forEach { key, value ->
                        println(key + "=" + value)
                    }
                }
            }
        """.trimIndent())

        val buildResult = runTasksSuccessfully("printSonarqubeProperties")
        assertThat(buildResult.output.lines(), hasItem("sonar.projectKey=projectKey"))
        assertThat(buildResult.output.lines(), hasItem("sonar.branch.name=feature/BACKEND-2588_build_jar"))
        assertThat(buildResult.output.lines(), hasItem("sonar.java.libraries="))
        assertThat(buildResult.output.lines(), hasItem("sonar.java.test.libraries="))
    }

    @Test
    fun `sonarqube should depend on jacoco and checkstyle tasks`() {
        buildFile.appendText("\n")
        buildFile.appendText("""
            task printDependsOnSonarqubeTasks {
                doLast {
                    project.tasks.getByName("sonarqube").dependsOn.forEach { println(it) }
                }
            }
        """.trimIndent())

        val buildResult = runTasksSuccessfully("printDependsOnSonarqubeTasks")
        assertThat(buildResult.output, containsString("checkstyleMain"))
        assertThat(buildResult.output, containsString("jacocoAggReport"))
        assertThat(buildResult.output, containsString("jacocoTestReport"))
    }

    @Test
    fun `analysis tasks should be disabled when analyseDevelopmentBranchesOnly true`() {
        buildFile.appendText("\n")
        buildFile.appendText("""
            javaModule.sonarqube.enabled = true
            
            task printAnalysisTasksStatus {
                doLast {
                    def sonarqube = project.tasks.getByName("sonarqube")
                    def checkstyle = project.tasks.getByName("checkstyleMain")
                    def spotbugs = project.tasks.getByName("spotbugsMain")
                    
                    println("sonarqube:" + sonarqube.onlyIf.isSatisfiedBy(sonarqube))
                    println("checkstyle:" + checkstyle.onlyIf.isSatisfiedBy(checkstyle))
                    println("spotbugs:" + spotbugs.onlyIf.isSatisfiedBy(spotbugs))
                }
            }
        """.trimIndent())

        git.checkout().setName("master").call()

        val buildResult = runTasksSuccessfully("printAnalysisTasksStatus")
        assertThat(buildResult.output, containsString("sonarqube:false"))
        assertThat(buildResult.output, containsString("checkstyle:false"))
        assertThat(buildResult.output, containsString("spotbugs:false"))
    }

    @Test
    fun `analysis tasks should be enabled when analyseDevelopmentBranchesOnly false`() {
        buildFile.appendText("\n")
        buildFile.appendText("""
            javaModule.sonarqube.enabled = true
            javaModule.analyseDevelopmentBranchesOnly = false
            
            task printAnalysisTasksStatus {
                doLast {
                    def sonarqube = project.tasks.getByName("sonarqube")
                    def checkstyle = project.tasks.getByName("checkstyleMain")
                    def spotbugs = project.tasks.getByName("spotbugsMain")
                    
                    println("sonarqube:" + sonarqube.onlyIf.isSatisfiedBy(sonarqube))
                    println("checkstyle:" + checkstyle.onlyIf.isSatisfiedBy(checkstyle))
                    println("spotbugs:" + spotbugs.onlyIf.isSatisfiedBy(spotbugs))
                }
            }
        """.trimIndent())

        git.checkout().setName("master").call()

        val buildResult = runTasksSuccessfully("printAnalysisTasksStatus")
        assertThat(buildResult.output, containsString("sonarqube:true"))
        assertThat(buildResult.output, containsString("checkstyle:true"))
        assertThat(buildResult.output, containsString("spotbugs:true"))
    }
}
