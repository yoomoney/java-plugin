package ru.yandex.money.gradle.plugins.backend.build

/**
 * Расширения для плагина
 *
 * @author Andrey Mochalov
 * @since 06.05.2019
 */
open class JavaModuleExtension {

    companion object {
        private const val DEFAULT_CHECKSTYLE_ENABLED = true
        private const val DEFAULT_SPOTBUGS_ENABLED = true
    }

    var checkstyleEnabled: Boolean = DEFAULT_CHECKSTYLE_ENABLED
    var spotbugsEnabled = DEFAULT_SPOTBUGS_ENABLED

    var test = TestNgExtension()
    var componentTest = TestNgExtension()

    var additionalRepo: List<String> = emptyList()
}
