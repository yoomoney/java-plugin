package ru.yoomoney.gradle.plugins.backend.build.kotlin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Конфигурация kotlin плагина
 *
 * @author Valerii Zhirnov
 * @since 17.04.2019
 */
class KotlinConfigurer {

    fun init(target: Project) {
        val sourceSets = target.extensions.getByType(JavaPluginExtension::class.java).sourceSets
        target.plugins.apply(KotlinPluginWrapper::class.java)

        target.tasks.withType(KotlinCompile::class.java).configureEach {
            sourceSets.getByName("main").output.classesDirs.forEach { it.mkdirs() }
            it.kotlinOptions.jvmTarget = "1.8"
        }
        target.extensions.getByType(JavaPluginExtension::class.java).apply {
            sourceSets.getByName("test").java.srcDirs += target.file("src/test/kotlin")
        }
    }
}
