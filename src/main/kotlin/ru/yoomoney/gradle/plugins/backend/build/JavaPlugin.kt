package ru.yoomoney.gradle.plugins.backend.build

import com.github.spotbugs.snom.SpotBugsBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.sonarqube.gradle.SonarQubePlugin
import ru.yoomoney.gradle.plugins.backend.build.checkstyle.CheckCheckstyleConfigurer
import ru.yoomoney.gradle.plugins.backend.build.coverage.CoverageConfigurer
import ru.yoomoney.gradle.plugins.backend.build.git.GitFlowConfigurer
import ru.yoomoney.gradle.plugins.backend.build.idea.IdeaPluginConfigurer
import ru.yoomoney.gradle.plugins.backend.build.jar.JarConfigurer
import ru.yoomoney.gradle.plugins.backend.build.kotlin.KotlinConfigurer
import ru.yoomoney.gradle.plugins.backend.build.sonarqube.SonarqubeConfigurer
import ru.yoomoney.gradle.plugins.backend.build.spotbugs.SpotBugsAnnotationsConfigurer
import ru.yoomoney.gradle.plugins.backend.build.spotbugs.SpotBugsConfigurer
import ru.yoomoney.gradle.plugins.backend.build.test.TestConfigurer

/**
 * Плагин для сборки модулей компонента
 *
 * @author Valerii Zhirnov
 * @since 22.03.2019
 */
class JavaPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.buildDir = target.file("target")

        target.pluginManager.apply(JavaPlugin::class.java)
        target.pluginManager.apply(GroovyPlugin::class.java)
        target.pluginManager.apply(IdeaPlugin::class.java)
        target.pluginManager.apply(JacocoPlugin::class.java)
        target.pluginManager.apply(CheckstylePlugin::class.java)
        target.pluginManager.apply(SpotBugsBasePlugin::class.java)
        target.pluginManager.apply(SonarQubePlugin::class.java)

        target.extensions.create("javaModule", JavaExtension::class.java)

        GitFlowConfigurer().init(target)
        JarConfigurer().init(target)
        TestConfigurer().init(target)
        KotlinConfigurer().init(target)
        IdeaPluginConfigurer().init(target)
        SpotBugsConfigurer().init(target)
        SpotBugsAnnotationsConfigurer().init(target)
        CoverageConfigurer().init(target)
        CheckCheckstyleConfigurer().init(target)
        SonarqubeConfigurer().init(target)
    }
}
