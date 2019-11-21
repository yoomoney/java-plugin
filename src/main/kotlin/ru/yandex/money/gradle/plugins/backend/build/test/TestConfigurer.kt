package ru.yandex.money.gradle.plugins.backend.build.test

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.testng.TestNGOptions

/**
 * Конфигрурует unit тесты и компонетные тесты
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 17.04.2019
 */
class TestConfigurer {

    fun init(target: Project) {
        target.tasks.create("testJunit", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
        }
        target.tasks.maybeCreate("test", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = "classes"
                it.threadCount = 8
            }
            dependsOn("testJunit")
        }
        val chosenSourceSet = if (target.file("src/componentTest").exists()) "componentTest" else "slowTest"
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            val componentTest = sourceSets.create(chosenSourceSet)
            componentTest.compileClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            componentTest.runtimeClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            componentTest.java { it.srcDir(target.file("src/$chosenSourceSet/java")) }
            componentTest.resources {
                it.srcDir(target.file("src/$chosenSourceSet/resources"))
            }
        }

        target.configurations
                .getByName("${chosenSourceSet}Compile")
                .extendsFrom(target.configurations.getByName("testCompile"))
        target.configurations
                .getByName("${chosenSourceSet}Runtime")
                .extendsFrom(target.configurations.getByName("testRuntime"))
        target.tasks.create("${chosenSourceSet}TestNg", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = "classes"
                it.threadCount = 8
            }
            val slowTest = target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getAt(chosenSourceSet)
            testClassesDirs = slowTest.output.classesDirs
            classpath = slowTest.runtimeClasspath
        }
        target.tasks.create("${chosenSourceSet}Test", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
            val slowTest = target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getAt(chosenSourceSet)
            testClassesDirs = slowTest.output.classesDirs
            classpath = slowTest.runtimeClasspath
            dependsOn("${chosenSourceSet}TestNg")
        }
        target.tasks.create("componentTest", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
            dependsOn("${chosenSourceSet}Test")
        }
        target.tasks.withType(Test::class.java).forEach {
            it.reports.junitXml.destination = target.file("${target.property("testResultsDir")}/${it.name}")
            it.reports.html.destination = target.file("${target.buildDir}/reports/${it.name}")
        }
        val compileTestJavaTaskName = if (target.file("src/componentTest").exists()) "compileComponentTestJava" else "compileSlowTestJava"
        target.tasks.getByName("check").apply {
            dependsOn -= "componentTest"
            dependsOn -= "${chosenSourceSet}Test"
            dependsOn -= "${chosenSourceSet}TestNg"
            dependsOn += compileTestJavaTaskName
        }
    }
}
