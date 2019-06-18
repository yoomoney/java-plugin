package ru.yandex.money.gradle.plugins.backend.build.kotlin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Конфигурация kotlin плагина
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 17.04.2019
 */
class KotlinConfigurer {

    fun init(target: Project) {
        target.plugins.apply(KotlinPluginWrapper::class.java)
        target.dependencies.add(
                "testCompile",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${KotlinVersion.CURRENT}"
        )
        target.dependencies.add(
                "testCompile",
                "org.jetbrains.kotlin:kotlin-reflect:${KotlinVersion.CURRENT}"
        )
        target.tasks.withType(KotlinCompile::class.java).configureEach {
            it.kotlinOptions.jvmTarget = "1.8"
        }
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceSets.getByName("test").java.srcDirs += target.file("src/test/kotlin")
        }
        target.afterEvaluate {
            target.tasks.getByName("compileJava").dependsOn.remove(target.tasks.getByName("compileKotlin"))
        }
    }
}
