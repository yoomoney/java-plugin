package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import ru.yandex.money.gradle.plugins.backend.build.errorprone.ErrorProneConfigurer
import ru.yandex.money.gradle.plugins.backend.build.idea.IdeaPluginConfigurer
import ru.yandex.money.gradle.plugins.backend.build.jar.JarConfigurer

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
        target.pluginManager.apply(IdeaPlugin::class.java)

        target.beforeEvaluate {
            JarConfigurer().init(it)
            ErrorProneConfigurer().init(it)
            IdeaPluginConfigurer().init(it)
        }
    }
}
