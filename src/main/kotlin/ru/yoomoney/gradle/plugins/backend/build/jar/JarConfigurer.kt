package ru.yoomoney.gradle.plugins.backend.build.jar

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension
import ru.yoomoney.gradle.plugins.backend.build.git.GitManager
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Конфигурирует сборку jar файла
 *
 * @author Valerii Zhirnov
 * @since 16.04.2019
 */
class JarConfigurer {

    fun init(target: Project) {
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
        target.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(optional)
        target.extensions.getByType(JavaPluginExtension::class.java).apply {
            sourceSets.getByName("main").compileClasspath =
                sourceSets.getByName("main")
                    .compileClasspath
                    .plus(target.configurations.getByName("optional"))
        }
    }

    private fun configureRepos(target: Project) {
        target.afterEvaluate {
            val javaExtension = target.extensions.getByType(JavaExtension::class.java)

            javaExtension.repositories
                    .forEach { repo -> target.repositories.maven { it.setUrl(repo) } }

            //    для фиче веток доступны снапшотные репозитории
            if (isDevelopmentBranch(target)) {
                javaExtension.snapshotsRepositories
                        .forEach { repo -> target.repositories.maven { it.setUrl(repo) } }
            }
        }
    }

    private fun isDevelopmentBranch(target: Project): Boolean = GitManager(target).isDevelopmentBranch()

    private fun targetJavaVersion(target: Project) {
        target.afterEvaluate {
            val javaExtension = target.extensions.getByType(JavaExtension::class.java)

            val targetJavaVersion = target
                    .findProperty(javaExtension.jvmVersionPropertyName)
                    ?.let { it.toString().trim().let { version -> JavaVersion.toVersion(version) } }
                    ?: JavaVersion.VERSION_1_8

            target.extensions.getByType(JavaPluginExtension::class.java).apply {
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
    }

    private fun forceEncoding(target: Project) {
        (target.tasks.getByName(COMPILE_JAVA_TASK_NAME) as JavaCompile).apply {
            options.encoding = "UTF-8"
        }
        (target.tasks.getByName("compileTestJava") as JavaCompile).apply {
            options.encoding = "UTF-8"
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
}
