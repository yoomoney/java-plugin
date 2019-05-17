package ru.yandex.money.gradle.plugins.backend.build.errorprone

import net.ltgt.gradle.errorprone.ErrorPronePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Конфигурация errorprone
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 16.05.2019
 */
class ErrorProneConfigurer {

    fun init(target: Project) {
        val toolVersion = if (JavaVersion.current().isJava8) "2.0.19" else "2.3.2"
        target.pluginManager.apply(ErrorPronePlugin::class.java)
        target.dependencies.add(
                "errorprone",
                "com.google.errorprone:error_prone_core:$toolVersion"
        )
        target.tasks.withType(JavaCompile::class.java).configureEach {
            it.options.compilerArgs = it.options.compilerArgs + "-XepDisableAllChecks"
        }
    }
}
