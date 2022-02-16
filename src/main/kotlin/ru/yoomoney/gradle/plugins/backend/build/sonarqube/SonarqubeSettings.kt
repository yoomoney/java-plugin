package ru.yoomoney.gradle.plugins.backend.build.sonarqube

/**
 * Типизированные настройки плагина статического анализатора SonarQube
 *
 * @author Petr Zinin pgzinin@yoomoney.ru
 * @since 14.02.2022
 */
class SonarqubeSettings {
    /**
     * Флаг, включающий статический анализатор
     */
    var enabled: Boolean = false

    /**
     * Адрес сервера
     */
    var host: String? = null

    /**
     * Ключ-идентификатор проекта в SonarQube
     */
    var projectKey: String? = null

    /**
     * Токен к серверу SonarQube.
     * <p>
     * Используется для получения правил статического анализатора и отправки результатов анализа
     */
    var token: String? = null

    /**
     * Флаг включающий передачу путей к внешним зависимостям(артефактам).
     * <p>
     * Передача путей к внешним зависимостям(артефактам) повышает качество статического анализа, но в то же время
     * замедляет скорость его работы.
     */
    var supplyLibrariesPath: Boolean = true
}