package ru.yandex.money.gradle.plugins.backend.build.staticanalysis

import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors.toMap

/**
 * Представление ограничений статических анализаторов кода
 *
 * @author Anton Kuzmin
 */
class StaticAnalysisProperties(
    /** Файл с ограничениями */
        val file: File,
    /** Ограничение на количество compiler */
        var compiler: Int?,
    /** Ограничение на количество checkstyle */
        var checkstyle: Int?,
    /** Ограничение на количество findbugs */
        var findbugs: Int?
) {

    companion object {
        private const val COMPILER_KEY = "compiler"
        private const val CHECKSTYLE_KEY = "checkstyle"
        private const val FINDBUGS_KEY = "findbugs"

        private const val STATIC_ANALYSIS_FILE = "static-analysis.properties"

        /**
         * Загружает ограничения статических анализаторов в их представление
         *
         * @return представление ограничений статических анализаторов
         */
        fun load(project: Project): StaticAnalysisProperties? {
            val file = project.file(STATIC_ANALYSIS_FILE)
            if (!(file.exists() && file.isFile)) {
                project.logger.warn("File not found: name={}", STATIC_ANALYSIS_FILE)
                return null
            }

            val properties = Files.newBufferedReader(file.toPath()).lines()
                    .map { it.split("=") }
                    .collect(toMap(
                            { splitted: List<String> -> splitted[0] },
                            { splitted: List<String> -> splitted[1] }
                    ))

            return StaticAnalysisProperties(
                    file = file,
                    compiler = properties[COMPILER_KEY]?.toInt(),
                    checkstyle = properties[CHECKSTYLE_KEY]?.toInt(),
                    findbugs = properties[FINDBUGS_KEY]?.toInt()
            )
        }
    }

    /**
     * Сохраняет новые ограничения статических анализаторов в файл
     */
    fun store() {
        Files.newBufferedWriter(file.toPath()).use { writer ->
            compiler?.also { storeValue(writer, COMPILER_KEY, it) }
            checkstyle?.also { storeValue(writer, CHECKSTYLE_KEY, it) }
            findbugs?.also { storeValue(writer, FINDBUGS_KEY, it) }
        }
    }

    private fun storeValue(writer: BufferedWriter, key: String, value: Int) {
        writer.append("$key=$value")
        writer.newLine()
    }
}