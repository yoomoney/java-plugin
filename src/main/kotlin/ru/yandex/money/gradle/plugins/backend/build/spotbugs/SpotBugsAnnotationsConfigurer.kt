package ru.yandex.money.gradle.plugins.backend.build.spotbugs

import org.gradle.api.Project

/**
 * Добавляет проекту опциональные зависимости на аннотации SpotBugs
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 24.04.2019
 */
class  SpotBugsAnnotationsConfigurer {

    fun init(target: Project) {
        target.dependencies.add("optional", "com.github.spotbugs:spotbugs-annotations:3.1.12")
        target.dependencies.add("optional", "com.google.code.findbugs:jsr305:3.0.2")
        target.dependencies.add("optional", "net.jcip:jcip-annotations:1.0")
    }
}
