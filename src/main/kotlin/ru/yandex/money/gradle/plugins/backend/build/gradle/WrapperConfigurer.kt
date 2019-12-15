package ru.yandex.money.gradle.plugins.backend.build.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.wrapper.Wrapper

/**
 * Добавляет таску на иницаиализацию wrapper'а для gradle
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 15.05.2019
 */
class WrapperConfigurer {
    fun init(target: Project) {
        target.tasks.maybeCreate("wrapper", Wrapper::class.java).apply {
            distributionUrl = "https://nexus.yamoney.ru/content/repositories/" +
                    "http-proxy-services.gradle.org/distributions/gradle-6.0.1-all.zip"
        }
    }
}
