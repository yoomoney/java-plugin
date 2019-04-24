package ru.yandex.money.gradle.plugins.backend.build.jar

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.jvm.tasks.Jar
import ru.yandex.money.gradle.plugins.backend.build.git.GitManager
import java.net.InetAddress
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Конфигурирует сборку jar файла
 *
 * @author Valerii Zhirnov (vazhirnov@yamoney.ru)
 * @since 16.04.2019
 */
class JarConfigurer {

    fun init(target: Project) {
        configureArchiveName(target)
        forceEncoding(target)
        targetJavaVersion(target)
        configureRepos(target)
        optionalSourceSet(target)
        resolutionStrategy(target)
        wrapper(target)
        configureJar(target)
    }

    private fun configureJar(target: Project) {
        target.tasks.maybeCreate("jar", Jar::class.java).apply {
            val buildDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            manifest {
                it.attributes(
                        mapOf(
                                "Implementation-Version" to "built at $buildDate on ${getHostName()}",
                                "Bundle-SymbolicName" to target.name,
                                "Built-By" to System.getProperty("user.name"),
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

    private fun getHostName(): String {
        return InetAddress.getLocalHost().hostName
    }

    private fun wrapper(target: Project) {
        target.tasks.maybeCreate("wrapper", Wrapper::class.java).apply {
            distributionUrl = "https://nexus.yamoney.ru/content/repositories/http-proxy-services.gradle.org/distributions/gradle-4.10.2-all.zip"
        }
    }

    private fun resolutionStrategy(target: Project) {
        target.configurations.all { conf ->
            conf.resolutionStrategy { rs ->
                rs.cacheChangingModulesFor(0, TimeUnit.SECONDS)
                rs.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
            }
        }
    }

    private fun optionalSourceSet(target: Project) {
        val optional = target.configurations.create("optional")
        target.configurations.getByName("testCompile").setExtendsFrom(listOf(optional))
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceSets.getByName("main").compileClasspath =
                    sourceSets.getByName("main")
                            .compileClasspath
                            .plus(target.configurations.getByName("optional"))
        }
    }

    private fun configureRepos(target: Project) {
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
    }

    private fun isStableBranch(target: Project): Boolean {
        return !GitManager(target).isFeatureBranch()
    }

    private fun targetJavaVersion(target: Project) {
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    private fun forceEncoding(target: Project) {
        (target.tasks.getByName("compileJava") as JavaCompile).apply {
            options.encoding = "UTF-8"
        }
        (target.tasks.getByName("compileTestJava") as JavaCompile).apply {
            options.encoding = "UTF-8"
        }
    }

    private fun configureArchiveName(target: Project) {
        target.convention.getPlugin(BasePluginConvention::class.java).apply {
            archivesBaseName = archivesBaseName.replace("\\bmoney-", "")
            archivesBaseName = when {
                archivesBaseName.startsWith("yamoney-") -> archivesBaseName
                else -> "yamoney-$archivesBaseName"
            }
        }
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