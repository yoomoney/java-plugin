package ru.yandex.money.gradle.plugins.backend.build

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.io.File
import java.util.Properties

/**
 * TODO:
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
    fun `should run kotlin test`() {
        val buildResult = runTasksSuccessfully("clean", "test", "build")
        assertThat("Java tests passed", buildResult.output, containsString("run java test..."))
        assertThat("Kotlin tests passed", buildResult.output, containsString("run kotlin test..."))
    }
}
