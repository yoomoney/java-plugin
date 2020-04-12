package ru.yandex.money.gradle.plugins.backend.build.checkdependencies

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.Project
import ru.yandex.money.gradle.plugins.backend.build.nexus.NexusUtils
import ru.yandex.money.gradle.plugins.library.dependencies.CheckDependenciesPluginExtension
import ru.yandex.money.gradle.plugins.library.dependencies.checkversion.MajorVersionCheckerExtension
import ru.yandex.money.gradle.plugins.library.dependencies.dsl.LibraryName
import ru.yandex.money.gradle.plugins.library.dependencies.forbiddenartifacts.ForbiddenDependenciesExtension
import java.util.HashSet

/**
 * Конфигурация yamoney-check-dependencies-plugin
 *
 * @author Oleg Kandaurov
 * @since 08.05.2019
 */
class CheckDependenciesConfigurer {

    private val librariesDependencies: LibraryName =
            LibraryName("ru.yandex.money.platform", "yamoney-libraries-dependencies")

    fun init(target: Project) {
        configureCheckDependenciesExtension(target)
        configureMajorVersionCheckerExtension(target)
        configurePlatformDependencies(target)
        configureForbiddenDependencies(target)
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
            val platformDependenciesVersion = getPlatformDependenciesVersion()

            if (platformDependenciesVersion != null) {
                project.logger.lifecycle("Version \"ru.yandex.money.platform:yamoney-libraries-dependencies\" resolved: " +
                        "$platformDependenciesVersion")
                imports {
                    it.mavenBom(
                            "${librariesDependencies.group}:${librariesDependencies.name}:$platformDependenciesVersion"
                    )
                }
            }
        }
    }

    private fun getPlatformDependenciesVersion(): String? {
        val platformDependenciesVersion: String? = System.getProperty("platformDependenciesVersion")

        if (platformDependenciesVersion?.contains('+') ?: false) {
            return NexusUtils.resolveVersion(librariesDependencies.group, librariesDependencies.name,
                    platformDependenciesVersion!!)
        }
        return platformDependenciesVersion
    }

    private fun configureCheckDependenciesExtension(project: Project) {
        val checkDependenciesPluginExtension = project.extensions
                .findByType(CheckDependenciesPluginExtension::class.java)

        checkDependenciesPluginExtension!!.exclusionsRulesSources = listOf(
                "ru.yandex.money.platform:yamoney-libraries-dependencies",
                "libraries-versions-exclusions.properties"
        )
    }

    private fun configureForbiddenDependencies(project: Project) {
        val forbiddenDependencies = project.extensions.getByType(ForbiddenDependenciesExtension::class.java)

        forbiddenDependencies.run {
            range(ForbiddenDependenciesExtension.ForbiddenArtifactParameter()
                    .forbidden("ru.yandex.money.common:yamoney-http-client")
                    .startVersion("5.0.0")
                    .endVersion("5.0.1")
                    .recommended("5.1.+")
                    .comment("В версии есть ошибка конфигурирования socketTimeout и connectionTimeout"))

            before(ForbiddenDependenciesExtension.ForbiddenArtifactParameter()
                    .forbidden("ru.yandex.money.common:yamoney-db-utils:5.1.3")
                    .recommended("5.1.4+")
                    .comment("В версии 5.1.3 и ранее есть баг, вызывающий роллбэки"))

            eq(ForbiddenDependenciesExtension.ForbiddenArtifactParameter()
                    .forbidden("ru.yandex.money.common:yamoney-backend-platform-core:41.1.1")
                    .recommended("41.1.2+")
                    .comment("В версии 41.1.1 содержатся ошибки приводящие к падению приложения"))
        }
    }
}
