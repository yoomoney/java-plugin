package ru.yoomoney.gradle.plugins.backend.build

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
    var spotbugsEnabled = DEFAULT_SPOTBUGS_ENABLED

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
    var javaVersionPropertyName: String = "java-plugin.jvm-version"

    /**
     * url для скачивания дистрибутива gradle, используется для wrapper таски.
     */
    var gradleDistributionUrl: String = "https://services.gradle.org/distributions/gradle-6.4.1-all.zip"

    /**
     * Префикс для имени jar. Будет добавлен для каждого jar, может содержать, например, имя организации.
     */
    var jarArchivePrefixName: String = ""
}
