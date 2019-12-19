package ru.yandex.money.gradle.plugins.backend.build.jar

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.jvm.Jvm
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
        configureJar(target)
        enableCompileJavaFork(target)
    }

    private fun configureJar(target: Project) {
        val jarTask = target.tasks.getByName("jar") as Jar
        jarTask.apply {
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

    private fun getHostName(): String = InetAddress.getLocalHost().hostName

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
        target.configurations.getByName("testCompile").extendsFrom(optional)
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
        if (isDevelopmentBranch(target)) {
            target.repositories.mavenLocal()
            target.repositories.maven { it.url = repos.getValue("snapshots") }
            target.repositories.maven { it.url = repos.getValue("spp-snapshots") }
        }
        target.repositories.maven { it.url = repos.getValue("spp-releases") }
        target.repositories.maven { it.url = repos.getValue("thirdparty") }
        target.repositories.maven { it.url = repos.getValue("central") }
    }

    private fun isDevelopmentBranch(target: Project): Boolean = GitManager(target).isDevelopmentBranch()

    private fun targetJavaVersion(target: Project) {
        val targetJavaVersion = target
            .findProperty("yamoney.java-module-plugin.jvm.version")
            ?.let { it.toString().trim().let { version -> JavaVersion.toVersion(version) } }
            ?: JavaVersion.VERSION_1_8

        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceCompatibility = targetJavaVersion
            targetCompatibility = targetJavaVersion
        }

        val currentCompileJavaVersion = Jvm.current().javaVersion
        if (currentCompileJavaVersion == null || currentCompileJavaVersion.isJava9Compatible) {
            (target.tasks.getByName(COMPILE_JAVA_TASK_NAME) as JavaCompile).apply {
                options.compilerArgs.addAll(listOf("--release", targetJavaVersion.majorVersion))
            }
        }
    }

    private fun forceEncoding(target: Project) {
        (target.tasks.getByName(COMPILE_JAVA_TASK_NAME) as JavaCompile).apply {
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

    private fun enableCompileJavaFork(target: Project) {
        val javaHomePath = getJavaHomePath()
        if (javaHomePath != null) {
            (target.tasks.getByName(COMPILE_JAVA_TASK_NAME) as JavaCompile).apply {
                options.isFork = true
                options.forkOptions.javaHome = target.file(javaHomePath)
            }
        }
    }

    private fun getJavaHomePath(): String? {
        return if (Jvm.current() != null) {
            Jvm.current().javaHome.absolutePath
        } else if (System.getProperty("java.home") != null) {
            System.getProperty("java.home")
        } else {
            System.getenv("JAVA_HOME")
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
