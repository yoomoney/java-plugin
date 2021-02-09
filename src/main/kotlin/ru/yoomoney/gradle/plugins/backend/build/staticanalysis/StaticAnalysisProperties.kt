package ru.yoomoney.gradle.plugins.backend.build.staticanalysis

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
class StaticAnalysisProperties
private constructor(
    /** Файл с ограничениями */
        val file: File,
    /** Ограничения */
        val properties: MutableMap<String, Int>
) {

    companion object {
        const val COMPILER_KEY = "compiler"
        const val CHECKSTYLE_KEY = "checkstyle"
        const val FINDBUGS_KEY = "findbugs"

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
                    .filter { !it.startsWith("#") }
                    .map { it.split("=") }
                    .collect(toMap(
                            { splitted: List<String> -> splitted[0] },
                            { splitted: List<String> -> splitted[1].toInt() }
                    ))

            return StaticAnalysisProperties(file, properties)
        }
    }

    /**
     * Получает значение ограничения по его ключу
     */
    fun getProperty(key: String) = properties[key]

    /**
     * Устанавливает значение ограничения
     */
    fun setProperty(key: String, value: Int) {
        properties[key] = value
    }

    /**
     * Сохраняет новые ограничения статических анализаторов в файл
     */
    fun store() {
        Files.newBufferedWriter(file.toPath()).use { writer ->
            properties.entries.forEach { storeValue(writer, it.key, it.value) }
        }
    }

    private fun storeValue(writer: BufferedWriter, key: String, value: Int) {
        writer.append("$key=$value")
        writer.newLine()
    }
}