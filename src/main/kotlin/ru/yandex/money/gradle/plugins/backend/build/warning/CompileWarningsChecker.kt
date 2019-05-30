package ru.yandex.money.gradle.plugins.backend.build.warning

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import ru.yandex.money.gradle.plugins.backend.build.getStaticAnalysisLimit
import ru.yandex.money.gradle.plugins.backend.build.git.GitManager

/**
 * Класс для проверки, что количество compile warnings не превышает заданное в файле static-analysis.properties
 *
 * @author Andrey Mochalov
 * @since 16.04.2019
 */
open class CompileWarningsChecker {

    fun init(project: Project) {
        val limitOpt = getStaticAnalysisLimit(project, LIMIT_NAME)
        if (!limitOpt.isPresent) {
            project.logger.warn("No settings for compiler warnings check. Skipping.")
            return
        }

        val gitManager = GitManager(project)
        if (!gitManager.isDevelopmentBranch()) {
            project.logger.warn("Compiler warnings check is enabled on feature/ and hotfix/ and release/ branches. " +
                    "Skipping.")
            return
        }

        val compileErrorOutDirPath = "${project.buildDir}/reports/compileJava/"
        val compileErrorOutFilePath = compileErrorOutDirPath + "compile_error_out.txt"

        val compileJavaTask = project.tasks.findByName("compileJava") as JavaCompile
        compileJavaTask.options.isDeprecation = false
        compileJavaTask.options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xmaxwarns", "10000", "-Xstdout", compileErrorOutFilePath))

        compileJavaTask.doFirst {
            project.mkdir(compileErrorOutDirPath)
        }

        compileJavaTask.doLast {
            CompileWarningsChecker().checkCompileWarnings(project, compileErrorOutFilePath, limitOpt.get())
        }
    }

    private fun checkCompileWarnings(project: Project, compileErrorOutFilePath: String, limit: Int) {
        var warnCount = 0

        project.file(compileErrorOutFilePath).forEachLine {
            if (" warning: " in it) {
                warnCount++
            }

            // Вывод в system.out
            println(it)
        }

        when {
            warnCount > limit -> throw GradleException("Too much compiler warnings: actual=$warnCount, limit=$limit")
            warnCount < getCompilerLowerLimit(limit) -> throw GradleException("Compiler warnings limit is too high, " +
                    "must be $warnCount. Decrease it in file static-analysis.properties.")
            else -> project.logger.lifecycle("Compiler warnings check successfully passed with $warnCount warnings")
        }
    }

    private fun getCompilerLowerLimit(limit: Int) = limit * 95 / 100

    companion object {
        private const val LIMIT_NAME = "compiler"
    }
}
