package ru.yandex.money.gradle.plugins.backend.build

import org.testng.annotations.Test
import java.io.File

/**
 * TODO:
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 16.04.2019
 */
class JavaModulePluginTest : AbstractPluginTest() {

    @Test
    fun test() {
        runTasksSuccessfully("clean", "build", "jar")
        assertFileExists(File("asd"))
    }
}