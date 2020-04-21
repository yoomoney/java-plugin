package ru.yandex.money.gradle.plugins.backend.build

import com.github.spotbugs.snom.SpotBugsBasePlugin
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import ru.yandex.money.gradle.plugins.backend.build.checkdependencies.CheckDependenciesConfigurer
import ru.yandex.money.gradle.plugins.backend.build.checkstyle.CheckCheckstyleConfigurer
import ru.yandex.money.gradle.plugins.backend.build.coverage.CoverageConfigurer
import ru.yandex.money.gradle.plugins.backend.build.git.GitFlowConfigurer
import ru.yandex.money.gradle.plugins.backend.build.gradle.WrapperConfigurer
import ru.yandex.money.gradle.plugins.backend.build.idea.IdeaPluginConfigurer
import ru.yandex.money.gradle.plugins.backend.build.jar.JarConfigurer
import ru.yandex.money.gradle.plugins.backend.build.kotlin.KotlinConfigurer
import ru.yandex.money.gradle.plugins.backend.build.spotbugs.SpotBugsAnnotationsConfigurer
import ru.yandex.money.gradle.plugins.backend.build.spotbugs.SpotBugsConfigurer
import ru.yandex.money.gradle.plugins.backend.build.test.TestConfigurer
import ru.yandex.money.gradle.plugins.library.dependencies.CheckDependenciesPlugin

/**
 * Плагин для сборки модулей компонента
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 22.03.2019
 */
class JavaModulePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.buildDir = target.file("target")

        target.pluginManager.apply(JavaPlugin::class.java)
        target.pluginManager.apply(GroovyPlugin::class.java)
        target.pluginManager.apply(IdeaPlugin::class.java)
        target.pluginManager.apply(JacocoPlugin::class.java)
        target.pluginManager.apply(DependencyManagementPlugin::class.java)
        target.pluginManager.apply(CheckDependenciesPlugin::class.java)
        target.pluginManager.apply(CheckstylePlugin::class.java)
        target.pluginManager.apply(SpotBugsBasePlugin::class.java)

        target.extensions.create("javaModule", JavaModuleExtension::class.java)

        WrapperConfigurer().init(target)
        GitFlowConfigurer().init(target)
        JarConfigurer().init(target)
        TestConfigurer().init(target)
        KotlinConfigurer().init(target)
        IdeaPluginConfigurer().init(target)
        SpotBugsConfigurer().init(target)
        SpotBugsAnnotationsConfigurer().init(target)
        CoverageConfigurer().init(target)
        CheckDependenciesConfigurer().init(target)
        CheckCheckstyleConfigurer().init(target)
    }
}
