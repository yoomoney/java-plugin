package ru.yandex.money.gradle.plugins.backend.build

import org.testng.annotations.Test

/**
 * TODO:
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 16.04.2019
 */
class JavaModulePluginTest : AbstractPluginTest() {

    @Test
    fun `should successfully run jar task`() {
        runTasksSuccessfully("clean", "build", "jar", "slowTest")
        //TODO: check manifest, jar file, markdown files
    }
}