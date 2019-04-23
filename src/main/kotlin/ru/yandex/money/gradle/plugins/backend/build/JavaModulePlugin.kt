package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import ru.yandex.money.gradle.plugins.backend.build.errorprone.ErrorProneConfigurer
import ru.yandex.money.gradle.plugins.backend.build.git.GitFlowConfigurer
import ru.yandex.money.gradle.plugins.backend.build.idea.IdeaPluginConfigurer
import ru.yandex.money.gradle.plugins.backend.build.jar.JarConfigurer
import ru.yandex.money.gradle.plugins.backend.build.kotlin.KotlinConfigurer
import ru.yandex.money.gradle.plugins.backend.build.library.LibraryProjectConfigurer
import ru.yandex.money.gradle.plugins.backend.build.test.TestConfigurer

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

        GitFlowConfigurer().init(target)
        JarConfigurer().init(target)
        TestConfigurer().init(target)
        KotlinConfigurer().init(target)

        target.beforeEvaluate {
            ErrorProneConfigurer().init(it)
            IdeaPluginConfigurer().init(it)
            LibraryProjectConfigurer().init(it)
        }
    }
}
