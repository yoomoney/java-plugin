package ru.yandex.money.gradle.plugins.backend.build.test

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.testng.TestNGOptions

/**
 * TODO:
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
                it.parallel = "tests"
                it.threadCount = 10
            }
            dependsOn("testJunit")
        }
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            val slowTest = sourceSets.create("slowTest")
            slowTest.java { it.srcDir(target.file("src/slowTest/java")) }
            slowTest.compileClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            slowTest.runtimeClasspath += sourceSets.getByName("main").output + sourceSets.getByName("test").output
            slowTest.resources { it.srcDir(target.file("src/slowTest/resources")) }
        }
        target.configurations
                .maybeCreate("slowTestCompile")
                .extendsFrom(target.configurations.getByName("testCompile"))
        target.configurations
                .maybeCreate("slowTestRuntime")
                .extendsFrom(target.configurations.getByName("testRuntime"))
        target.tasks.create("slowTestTestNg", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = "tests"
                it.threadCount = 10
            }
            val slowTest = target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getAt("slowTest")
            testClassesDirs = slowTest.output.classesDirs
            classpath = slowTest.runtimeClasspath
        }
        target.tasks.create("slowTest", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
            val slowTest = target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getAt("slowTest")
            testClassesDirs = slowTest.output.classesDirs
            classpath = slowTest.runtimeClasspath
            dependsOn("slowTestTestNg")
        }
        target.tasks.withType(Test::class.java).forEach {
            it.reports.junitXml.destination = target.file("${target.property("testResultsDir")}/${it.name}")
            it.reports.html.destination = target.file("${target.buildDir}/reports/${it.name}")
        }
        target.tasks.getByName("check").apply {
            dependsOn -= "slowTest"
            dependsOn -= "slowTestTestNg"
            dependsOn += "compileSlowTestJava"
        }
    }
}
