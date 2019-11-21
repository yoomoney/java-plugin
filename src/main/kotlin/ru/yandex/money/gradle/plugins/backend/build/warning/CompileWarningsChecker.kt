package ru.yandex.money.gradle.plugins.backend.build.warning

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import ru.yandex.money.gradle.plugins.backend.build.git.GitManager
import ru.yandex.money.gradle.plugins.backend.build.staticanalysis.StaticAnalysisProperties

/**
 * Класс для проверки, что количество compile warnings не превышает заданное в файле static-analysis.properties
 *
 * @author Andrey Mochalov
 * @since 16.04.2019
 */
open class CompileWarningsChecker {

    fun init(project: Project) {
        val staticAnalysis = StaticAnalysisProperties.load(project)

        val compilerLimit = staticAnalysis?.compiler
        if (compilerLimit == null) {
            project.logger.warn("No settings for compiler warnings check. Skipping.")
            return
        }

        val gitManager = GitManager(project)
        if (!gitManager.isDevelopmentBranch()) {
            project.logger.warn("Compiler warnings check is enabled on feature/ and hotfix/ and release/ branches. " +
                    "Skipping.")
            return
        }

        val compileErrorOutDirPath = "${project.buildDir}/$REPORT_DIR_PATH"
        val compileErrorOutFilePath = "$compileErrorOutDirPath/$ERROR_OUT_FILE_NAME"

        val compileJavaTask = project.tasks.findByName("compileJava") as JavaCompile
        compileJavaTask.options.isDeprecation = false
        compileJavaTask.options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xmaxwarns", "10000", "-Xstdout", compileErrorOutFilePath))

        compileJavaTask.doFirst {
            project.mkdir(compileErrorOutDirPath)
        }

        compileJavaTask.doLast {
            checkCompileWarnings(project, staticAnalysis, compileErrorOutFilePath, compilerLimit)
        }

        // Вывод error и warn при компиляции в system.out
        project.gradle.taskGraph.afterTask { task ->
            if (task is JavaCompile && task.name == "compileJava") {
                val errorOutFile = project.file(compileErrorOutFilePath)
                if (errorOutFile.exists()) {
                    project.file(compileErrorOutFilePath).forEachLine { println(it) }
                }
            }
        }
    }

    private fun checkCompileWarnings(project: Project, staticAnalysis: StaticAnalysisProperties, compileErrorOutFilePath: String, limit: Int) {
        var warnCount = 0

        project.file(compileErrorOutFilePath).forEachLine {
            if (" warning: " in it) {
                warnCount++
            }
        }

        when {
            warnCount > limit -> throw GradleException("Too much compiler warnings: actual=$warnCount, limit=$limit")
            warnCount < getCompilerLowerLimit(limit) -> updateIfLocalOrElseThrow(project, staticAnalysis, warnCount)
            else -> logSuccess(project, warnCount)
        }
    }

    private fun getCompilerLowerLimit(limit: Int) = limit * 95 / 100

    private fun updateIfLocalOrElseThrow(project: Project, staticAnalysis: StaticAnalysisProperties, warnCount: Int) {
        if (!project.hasProperty("ci")) {
            staticAnalysis.compiler = warnCount
            staticAnalysis.store()

            logSuccess(project, warnCount)
        } else {
            throw GradleException("Compiler warnings limit is too high, " +
                    "must be $warnCount. Decrease it in file static-analysis.properties.")
        }
    }

    private fun logSuccess(project: Project, warnCount: Int) {
        project.logger.lifecycle("Compiler warnings check successfully passed with $warnCount warnings")
    }

    companion object {
        private const val REPORT_DIR_PATH = "reports/compileJava"
        private const val ERROR_OUT_FILE_NAME = "compile_error_out.txt"
    }
}
