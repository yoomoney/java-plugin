package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.api.Project

/**
 * Расширения для плагина
 *
 * @author Andrey Mochalov
 * @since 06.05.2019
 */
open class JavaModuleExtensions(project: Project) {

    companion object {
        private const val DEFAULT_CHECKSTYLE_ENABLED = true
    }

    var checkstyleEnabled = DEFAULT_CHECKSTYLE_ENABLED
}