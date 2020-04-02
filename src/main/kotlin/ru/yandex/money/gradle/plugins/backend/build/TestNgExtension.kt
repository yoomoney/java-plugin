package ru.yandex.money.gradle.plugins.backend.build

import java.util.HashSet
import java.util.LinkedHashSet

/**
 * Расширения настроек TestNG для плагина.
 *
 * @author churkin
 * @since 14.04.2020
 */
open class TestNgExtension {

    /**
     * Набор классов слушателей событий выполнения тестов.
     *
     * @see org.gradle.api.tasks.testing.testng.TestNGOptions.listeners
     */
    var listeners: HashSet<String> = LinkedHashSet()
    /**
     * Количество потоков выполнения тестов.
     *
     * @see org.gradle.api.tasks.testing.testng.TestNGOptions.threadCount
     */
    var threadCount = 8
    /**
     * Режим параллльного выполнения тестов.
     *
     * @see org.gradle.api.tasks.testing.testng.TestNGOptions.parallel
     */
    var parallel = "classes"
}