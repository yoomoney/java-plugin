package ru.yoomoney.gradle.plugins.backend.build

import ru.yoomoney.gradle.plugins.backend.build.sonarqube.SonarqubeSettings

/**
 * Расширения для плагина
 *
 * @author Andrey Mochalov
 * @since 06.05.2019
 */
open class JavaExtension {

    companion object {
        private const val DEFAULT_CHECKSTYLE_ENABLED = true
        private const val DEFAULT_SPOTBUGS_ENABLED = true
    }

    /**
     * Флаг включения проверки ошибок checkstyle
     */
    var checkstyleEnabled: Boolean = DEFAULT_CHECKSTYLE_ENABLED

    /**
     * Флаг включения проверки ошибок spotbugs
     */
    var spotbugsEnabled: Boolean = DEFAULT_SPOTBUGS_ENABLED

    /**
     * Настройки статического анализатора SonarQube
     */
    var sonarqube: SonarqubeSettings = SonarqubeSettings()

    /**
     * Настройки статического анализатора SonarQube
     */
    var analyseDevelopmentBranchesOnly: Boolean = true

    /**
     * Настройки unit тестов. см. {@link TestNgExtension}
     */
    var test = TestNgExtension()

    /**
     * Настройки компонентных тестов. см. {@link TestNgExtension}
     */
    var componentTest = TestNgExtension()

    /**
     * Список репозиториев, которые будут добавлены в проект для поиска зависимостей
     */
    var repositories: List<String> = emptyList()
    /**
     * Список snapshots репозиториев, которые будут добавлены в проект для поиска зависимостей.
     * Будут добавлены только для фиче-веток
     */
    var snapshotsRepositories: List<String> = emptyList()

    /**
     * Имя gradle property, в которой можно указать версию целевую версию java для проекта
     */
    var jvmVersionPropertyName: String = "ru.yoomoney.gradle.plugins.java-plugin.jvm-version"
}
