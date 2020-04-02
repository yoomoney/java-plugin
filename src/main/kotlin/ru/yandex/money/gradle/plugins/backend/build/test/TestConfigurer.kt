package ru.yandex.money.gradle.plugins.backend.build.test

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.testng.TestNGOptions
import ru.yandex.money.gradle.plugins.backend.build.JavaModuleExtension

/**
 * Конфигрурует unit тесты и компонетные тесты
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 17.04.2019
 */
class TestConfigurer {

    fun init(target: Project) {
        val module = target.extensions.getByType(JavaModuleExtension::class.java)
        target.tasks.create("testJunit", Test::class.java).apply {
            systemProperty("file.encoding", "UTF-8")
        }
        target.tasks.maybeCreate("test", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = module.test.parallel
                it.threadCount = module.test.threadCount
                it.listeners = module.test.listeners
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
        val overwriteTestReportsTask = target.tasks.create("overwriteTestReports", OverwriteTestReportsTask::class.java).apply {
            xmlReportsPath = "${target.property("testResultsDir")}/${chosenSourceSet}TestNg"
        }
        target.tasks.create("${chosenSourceSet}TestNg", Test::class.java).apply {
            useTestNG()
            systemProperty("file.encoding", "UTF-8")
            options {
                it as TestNGOptions
                it.parallel = module.componentTest.parallel
                it.threadCount = module.componentTest.threadCount
                it.listeners = module.componentTest.listeners
            }
            val slowTest = target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getAt(chosenSourceSet)
            testClassesDirs = slowTest.output.classesDirs
            classpath = slowTest.runtimeClasspath
            finalizedBy(overwriteTestReportsTask)
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
            it.reports.junitXml.isOutputPerTestCase = true
            it.reports.html.destination = target.file("${target.buildDir}/reports/${it.name}")
        }
        val compileTestJavaTaskName = if (target.file("src/componentTest").exists()) "compileComponentTestJava" else "compileSlowTestJava"
        target.tasks.getByName("check").apply {
            dependsOn += compileTestJavaTaskName
        }
    }
}
