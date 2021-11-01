package ru.yoomoney.gradle.plugins.backend.build.test

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.testng.TestNGOptions
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension

/**
 * Конфигрурует unit тесты и компонетные тесты
 *
 * @author Valerii Zhirnov
 * @since 17.04.2019
 */
class TestConfigurer {
    companion object {
        public val ALL_TESTS_TASK_NAME = "unitAndComponentTests"
        private val UNIT_TESTS_TASK_NAME = "test"
        private val COMPONENT_TESTS_TASK_NAME = "componentTest"
        private val DEPRECATED_COMPONENT_TESTS_TASK_NAME = "slowTest"
    }

    fun init(target: Project) {
        val extension = target.extensions.getByType(JavaExtension::class.java)

        // Настройка unit тестов
        configureUnitTestTasks(target, extension)

        if (hasComponentTest(target)) {
            // Установка SourceSet для компонентных тестов
            val componentTestSourceSetName = setUpComponentTestsSourceSet(target)

            // Сохранение SourceSet для компонентных тестов в глобальную переменную с помощью механизма convention
            setGlobalSourceSet(target, componentTestSourceSetName)

            // Настройка компонентных тестов
            configureComponentTestTasks(target, extension, componentTestSourceSetName)

            // задача запуска всех существующих тестов
            target.tasks.create(ALL_TESTS_TASK_NAME).apply {
                dependsOn("test", "componentTest")
            }
        } else {
            // задача запуска всех существующих тестов
            target.tasks.create(ALL_TESTS_TASK_NAME).apply {
                dependsOn("test")
            }
        }

        target.tasks.withType(Test::class.java).forEach {
            it.reports.junitXml.destination = target.file("${target.property("testResultsDir")}/${it.name}")
            it.reports.junitXml.isOutputPerTestCase = true
            it.reports.html.destination = target.file("${target.buildDir}/reports/${it.name}")
        }
    }

    private fun hasComponentTest(target: Project): Boolean {
        return target.file("src/$COMPONENT_TESTS_TASK_NAME").exists() || target.file("src/$DEPRECATED_COMPONENT_TESTS_TASK_NAME").exists()
    }

    private fun setUpComponentTestsSourceSet(target: Project): String {
        val chosenSourceSetName = if (target.file("src/$COMPONENT_TESTS_TASK_NAME").exists()) COMPONENT_TESTS_TASK_NAME else DEPRECATED_COMPONENT_TESTS_TASK_NAME

        val compileTestJavaTaskName = "compile${chosenSourceSetName}Java"

        target.tasks.getByName("check").apply {
            dependsOn += compileTestJavaTaskName
        }

        target.configurations
            .getByName("${chosenSourceSetName}Compile")
            .extendsFrom(target.configurations.getByName("testCompile"))
        target.configurations
            .getByName("${chosenSourceSetName}Runtime")
            .extendsFrom(target.configurations.getByName("testRuntime"))

        return chosenSourceSetName
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
            dependsOn("testJunit", "testTestNG")
        }
    }

    private fun configureComponentTestTasks(target: Project, extension: JavaExtension, componentTestSourceSetName: String) {
        // Получение SourceSet для компонентных тестов из глобальной переменной с помощью механизма convention
        val sourceSet = getGlobalSourceSet(target, componentTestSourceSetName)

        // задача запуска компонентных Junit тестов
        target.tasks.create("${componentTestSourceSetName}Test", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
            testClassesDirs = sourceSet.output.classesDirs
            classpath = sourceSet.runtimeClasspath
        }

        val overwriteTestReportsTask = target.tasks.create("overwriteTestReports", OverwriteTestReportsTask::class.java).apply {
            xmlReportsPath = "${target.property("testResultsDir")}/${componentTestSourceSetName}TestNg"
        }

        // задача запуска компонентных TestNG тестов
        target.tasks.create("${componentTestSourceSetName}TestNg", Test::class.java).apply {
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

            finalizedBy(overwriteTestReportsTask)
        }

        // задача запуска компонентных TestNG и Junit тестов
        target.tasks.create("componentTest").apply {
            dependsOn("${componentTestSourceSetName}Test", "${componentTestSourceSetName}TestNg")
        }
    }

    private fun setGlobalSourceSet(target: Project, sourceSetName: String) {
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            val componentTest = sourceSets.create(sourceSetName)
            componentTest.compileClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            componentTest.runtimeClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            componentTest.java { it.srcDir(target.file("src/$sourceSetName/java")) }
            componentTest.resources {
                it.srcDir(target.file("src/$sourceSetName/resources"))
            }
        }
    }

    private fun getGlobalSourceSet(target: Project, sourceSetName: String): SourceSet {
        return target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getAt(sourceSetName)
    }
}
