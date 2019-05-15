package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.api.Project

/**
 * Расширения для плагина
 *
 * @author Andrey Mochalov
 * @since 06.05.2019
 */
open class JavaModuleExtension(project: Project) {

    companion object {
        private const val DEFAULT_CHECKSTYLE_ENABLED = true
        private const val DEFAULT_SPOTBUGS_ENABLED = true
    }

    var checkstyleEnabled: Boolean = DEFAULT_CHECKSTYLE_ENABLED
    var spotbugsEnabled = DEFAULT_SPOTBUGS_ENABLED
}
