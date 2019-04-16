package ru.yandex.money.gradle.plugins.backend.build.errorprone

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.net.URI

/**
 * TODO:
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 16.04.2019
 */
class ErrorProneConfigurer {

    fun init(target: Project) {
        target.buildscript.repositories.maven { it.url = URI("https://nexus.yamoney.ru/content/repositories/gradle-plugins/") }
        target.buildscript.dependencies.add("classpath", pluginVersion())
        target.apply(mapOf("plugin" to "net.ltgt.gradle.errorprone.ErrorPronePlugin"))
        target.configurations.maybeCreate("errorprone").resolutionStrategy.force(errorProneVersion())
        target.dependencies.add("errorprone", errorProneVersion())
    }

    private fun pluginVersion():String {
        return when {
            JavaVersion.current() <= JavaVersion.VERSION_1_8 -> "net.ltgt.gradle:gradle-errorprone-plugin:0.0.10"
            else -> "net.ltgt.gradle:gradle-errorprone-plugin:0.7"
        }
    }

    private fun errorProneVersion():String {
        return when {
            JavaVersion.current() <= JavaVersion.VERSION_1_8 -> "com.google.errorprone:error_prone_core:2.0.12"
            else -> "com.google.errorprone:error_prone_core:2.3.2"
        }
    }
}