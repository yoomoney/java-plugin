package ru.yandex.money.gradle.plugins.backend.build.platform

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.Project

/**
 * Подключет bom с платформенными библиотеками
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 17.04.2019
 */
class PlatformDependenciesConfigurer {

    fun init(target: Project) {
        target.extensions.getByType(DependencyManagementExtension::class.java).apply {
            overriddenByDependencies(false)
            generatedPomCustomization { customizationHandler -> customizationHandler.enabled(true) }
            val platformDependenciesVersion = System.getProperty("platformDependenciesVersion")
            if (platformDependenciesVersion != null) {
                imports {
                    it.mavenBom("ru.yandex.money.platform:yamoney-libraries-dependencies:$platformDependenciesVersion")
                }
            }
        }
    }
}
