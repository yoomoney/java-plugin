package ru.yoomoney.gradle.plugins.backend.build.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.wrapper.Wrapper
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension

/**
 * Добавляет таску на иницаиализацию wrapper'а для gradle
 *
 * @author Valerii Zhirnov
 * @since 15.05.2019
 */
class WrapperConfigurer {
    fun init(target: Project) {
        target.afterEvaluate {
            val javaPluginExtension = target.extensions.getByType(JavaExtension::class.java)
            target.tasks.maybeCreate("wrapper", Wrapper::class.java).apply {
                distributionUrl = javaPluginExtension.gradleDistributionUrl
            }
        }
    }
}
