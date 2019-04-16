package ru.yandex.money.gradle.plugins.backend.build

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import java.net.InetAddress
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Плагин для сборки модулей компонента
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 22.03.2019
 */
class JavaModulePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(JavaPlugin::class.java)
        target.buildDir = target.file("target")
        target.convention.getPlugin(BasePluginConvention::class.java).apply {
            archivesBaseName = archivesBaseName.replace("\\bmoney-", "")
            archivesBaseName = when {
                archivesBaseName.startsWith("yamoney-") -> archivesBaseName
                else -> "yamoney-$archivesBaseName"
            }
        }
        (target.tasks.getByName("compileJava") as JavaCompile).apply {
            options.encoding = "UTF-8"
        }
        (target.tasks.getByName("compileTestJava") as JavaCompile).apply {
            options.encoding = "UTF-8"
        }
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        target.extensions.extraProperties.set("schemaHome", "schema")
        target.extensions.extraProperties.set("artifactID", target.name)
        target.extensions.extraProperties.set("groupIdSuffix", "")
        target.repositories.maven { it.url = repos.getValue("releases") }
        target.repositories.maven { it.url = repos.getValue("jcenter") }
        if (!isStableBranch(target)) {
            target.repositories.mavenLocal()
            target.repositories.maven { it.url = repos.getValue("snapshots") }
            target.repositories.maven { it.url = repos.getValue("spp-snapshots") }
        }
        target.repositories.maven { it.url = repos.getValue("spp-releases") }
        target.repositories.maven { it.url = repos.getValue("thirdparty") }
        target.repositories.maven { it.url = repos.getValue("central") }
        target.configurations.maybeCreate("optional").apply {
            target.configurations.getByName("testCompile").setExtendsFrom(listOf(this))
        }
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceSets.getByName("main").compileClasspath =
                    sourceSets.getByName("main")
                            .compileClasspath
                            .plus(target.configurations.maybeCreate("optional"))
        }
        target.configurations.all { conf ->
            conf.resolutionStrategy { rs ->
                rs.cacheChangingModulesFor(0, TimeUnit.SECONDS)
                rs.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
            }
        }
        target.tasks.maybeCreate("wrapper", org.gradle.api.tasks.wrapper.Wrapper::class.java).apply {
            distributionUrl = "https://nexus.yamoney.ru/content/repositories/http-proxy-services.gradle.org/distributions/gradle-4.10.2-all.zip"
        }
        target.tasks.maybeCreate("jar", Jar::class.java).apply {
            val buildDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            manifest {
                it.attributes(
                        mapOf(
                                "Implementation-Version" to "built at $buildDate on ${getHostName()}",
                                "Bundle-SymbolicName" to target.name,
                                "Built-By" to java.lang.System.getProperty("user.name"),
                                "Built-Date" to buildDate,
                                "Built-At" to getHostName()
                        )
                )
            }
            from(target.projectDir) {
                it.include("CHANGELOG.md", "README.md")
                it.into("META-INF")
            }
        }

    }

    private fun isStableBranch(target: Project): Boolean {
        val props = listOf("isMasterOrDev", "isReleaseTag", "isReleaseBranch")
        for (prop in props) {
            if (target.hasProperty(prop) && target.property(prop) as Boolean) {
                return true
            }
        }
        return false
    }

    private fun getHostName(): String {
        return InetAddress.getLocalHost().hostName
    }

    companion object {
        val repos = mapOf(
                "central" to URI("https://nexus.yamoney.ru/content/repositories/central/"),
                "jcenter" to URI("https://nexus.yamoney.ru/content/repositories/jcenter.bintray.com/"),
                "snapshots" to URI("https://nexus.yamoney.ru/content/repositories/snapshots/"),
                "releases" to URI("https://nexus.yamoney.ru/content/repositories/releases/"),
                "thirdparty" to URI("https://nexus.yamoney.ru/content/repositories/thirdparty/"),
                "spp-snapshots" to URI("https://nexus.yamoney.ru/content/repositories/spp-snapshots/"),
                "spp-releases" to URI("https://nexus.yamoney.ru/content/repositories/spp-releases/")
        )
    }
}