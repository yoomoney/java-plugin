package ru.yandex.money.gradle.plugins.backend.build.checkdependencies

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.Project
import ru.yandex.money.gradle.plugins.library.dependencies.CheckDependenciesPluginExtension
import ru.yandex.money.gradle.plugins.library.dependencies.checkversion.MajorVersionCheckerExtension
import java.util.*

/**
 * Конфигурация yamoney-check-dependencies-plugin
 *
 * @author Oleg Kandaurov
 * @since 08.05.2019
 */
class CheckDependenciesConfigurer {

    fun init(target: Project) {
        configureCheckDependenciesExtension(target)
        configureMajorVersionCheckerExtension(target)
        configurePlatformDependencies(target)
    }

    private fun configureMajorVersionCheckerExtension(project: Project) {
        val includeGroupIdPrefixes = HashSet<String>()
        includeGroupIdPrefixes.add("ru.yamoney")
        includeGroupIdPrefixes.add("ru.yandex.money")

        project.extensions.findByType(MajorVersionCheckerExtension::class.java)!!
                .includeGroupIdPrefixes = includeGroupIdPrefixes
    }

    private fun configurePlatformDependencies(project: Project) {
        project.extensions.getByType(DependencyManagementExtension::class.java).apply {
            overriddenByDependencies(false)
            generatedPomCustomization { customizationHandler -> customizationHandler.enabled(true) }
            val platformDependenciesVersion = System.getProperty("platformDependenciesVersion")
            if (platformDependenciesVersion != null) {
                imports {
                    it.mavenBom("ru.yandex.money.platform:yamoney-libraries-dependencies:$platformDependenciesVersion")
                }
            }
        }
    }

    private fun configureCheckDependenciesExtension(project: Project) {
        val checkDependenciesPluginExtension = project.extensions.findByType(CheckDependenciesPluginExtension::class.java)

        checkDependenciesPluginExtension!!.excludedConfigurations = Arrays.asList(
                "checkstyle", "errorprone", "optional", "findbugs",
                "architecture", "architectureTestCompile", "architectureTestCompileClasspath",
                "architectureTestRuntime", "architectureTestRuntimeClasspath")

        checkDependenciesPluginExtension.exclusionsRulesSources = listOf("ru.yandex.money.platform:yamoney-libraries-dependencies",
                "libraries-versions-exclusions.properties")
    }
}