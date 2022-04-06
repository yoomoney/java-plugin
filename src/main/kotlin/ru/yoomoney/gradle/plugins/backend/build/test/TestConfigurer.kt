package ru.yoomoney.gradle.plugins.backend.build.test

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.testng.TestNGOptions
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension

/**
 * Конфигурирует unit тесты и компонентные тесты
 *
 * @author Valerii Zhirnov
 * @since 17.04.2019
 */
class TestConfigurer {
    companion object {
        const val ALL_TESTS_TASK_NAME = "unitAndComponentTest"
        const val UNIT_TEST_SOURCE_SET_NAME = "test"
        const val COMPONENT_TEST_SOURCE_SET_NAME = "componentTest"
        const val COMPONENT_TEST_DEPRECATED_SOURCE_SET_NAME = "slowTest"

        private const val UNIT_TESTS_TASK_NAME = "test"
        private const val COMPONENT_TESTS_TASK_NAME = "componentTest"
    }

    fun init(target: Project) {
        val extension = target.extensions.getByType(JavaExtension::class.java)

        configureUnitTestTasks(target, extension)

        configureComponentTestTasks(target, extension)

        // задача запуска всех существующих тестов
        target.tasks.create(ALL_TESTS_TASK_NAME).apply {
            dependsOn(UNIT_TESTS_TASK_NAME, COMPONENT_TESTS_TASK_NAME)
        }

        target.tasks.withType(Test::class.java).forEach {
            it.reports.junitXml.outputLocation.set(target.file("${target.property("testResultsDir")}/${it.name}"))
            it.reports.junitXml.isOutputPerTestCase = true
            it.reports.html.outputLocation.set(target.file("${target.buildDir}/reports/${it.name}"))
        }
    }

    private fun configureUnitTestTasks(target: Project, extension: JavaExtension) {
        // задача запуска Junit тестов
        target.tasks.create("testJunit", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
        }

        // задача запуска TestNG тестов
        target.tasks.create("testTestNG", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = extension.test.parallel
                it.threadCount = extension.test.threadCount
                it.listeners = extension.test.listeners
            }
        }

        // задача запуска TestNG и Junit тестов
        target.tasks.maybeCreate(UNIT_TESTS_TASK_NAME).apply {
            enabled = false
            dependsOn("testJunit", "testTestNG")
        }
    }

    private fun configureComponentTestTasks(target: Project, extension: JavaExtension) {
        val sourceSet = setUpComponentTestsSourceSet(target)

        // задача запуска компонентных Junit тестов
        target.tasks.create("${sourceSet.name}Test", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
            testClassesDirs = sourceSet.output.classesDirs
            classpath = sourceSet.runtimeClasspath
        }

        val overwriteTestReportsTask = OverwriteTestReportsTask(
            xmlReportsPath = "${target.property("testResultsDir")}/${sourceSet.name}TestNg",
            project = target
        )

        // задача запуска компонентных TestNG тестов
        target.tasks.create("${sourceSet.name}TestNg", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = extension.componentTest.parallel
                it.threadCount = extension.componentTest.threadCount
                it.listeners = extension.componentTest.listeners
            }

            testClassesDirs = sourceSet.output.classesDirs
            classpath = sourceSet.runtimeClasspath

            doLast {
                overwriteTestReportsTask.overwriteTestReports()
            }
        }

        // задача запуска компонентных TestNG и Junit тестов
        target.tasks.create("componentTest").apply {
            enabled = false
            dependsOn("${sourceSet.name}Test", "${sourceSet.name}TestNg")
        }

        val compileTestJavaTaskName = "compile${Character.toUpperCase(sourceSet.name[0])}${sourceSet.name.substring(1)}Java"

        target.tasks.getByName("check").apply {
            dependsOn += compileTestJavaTaskName
        }
    }

    private fun setUpComponentTestsSourceSet(target: Project): SourceSet {
        val chosenSourceSetName = if (target.file("src/$COMPONENT_TESTS_TASK_NAME").exists()) {
            COMPONENT_TEST_SOURCE_SET_NAME
        } else {
            COMPONENT_TEST_DEPRECATED_SOURCE_SET_NAME
        }

        // Создание и сохранение SourceSet для компонентных тестов в глобальную переменную с помощью механизма convention
        target.extensions.getByType(JavaPluginExtension::class.java).apply {
            val componentTest = sourceSets.create(chosenSourceSetName)
            componentTest.compileClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            componentTest.runtimeClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            componentTest.java { it.srcDir(target.file("src/$chosenSourceSetName/java")) }
            componentTest.resources {
                it.srcDir(target.file("src/$chosenSourceSetName/resources"))
            }
        }

        target.configurations
            .getByName("${chosenSourceSetName}Implementation")
            .extendsFrom(target.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME))
        target.configurations
            .getByName("${chosenSourceSetName}RuntimeOnly")
            .extendsFrom(target.configurations.getByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME))

        // Получение SourceSet для компонентных тестов из глобальной переменной с помощью механизма convention
        return target.extensions.getByType(JavaPluginExtension::class.java).sourceSets.getAt(chosenSourceSetName)
    }
}
