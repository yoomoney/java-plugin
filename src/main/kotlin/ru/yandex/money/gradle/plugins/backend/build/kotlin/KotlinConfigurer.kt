package ru.yandex.money.gradle.plugins.backend.build.kotlin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * TODO:
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 17.04.2019
 */
class KotlinConfigurer {

    fun init(target: Project) {
        if (System.getProperty("kotlinVersion") == null) {
            return
        }
        target.plugins.apply("kotlin")
        target.tasks.maybeCreate("compileKotlin", KotlinCompile::class.java).apply {
            kotlinOptions.jvmTarget = "1.8"
        }
        target.tasks.maybeCreate("compileTestKotlin", KotlinCompile::class.java).apply {
            kotlinOptions.jvmTarget = "1.8"
        }
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceSets.getByName("test").allSource.srcDirs += target.file("src/test/kotlin")
        }
    }
}