package ru.yoomoney.gradle.plugins.backend.build

import org.gradle.api.Project
import java.nio.file.Files
import java.util.Optional
import java.util.Properties

private const val LIMITS_FILE_NAME = "static-analysis.properties"

/**
 * Получение количества лимита нарушений
 *
 * @return количество допустимых нарушений
 */
internal fun getStaticAnalysisLimit(project: Project, limitName: String): Optional<Int> {
    val limitsFile = project.file(LIMITS_FILE_NAME)
    if (!(limitsFile.exists() && limitsFile.isFile)) {
        project.logger.warn("Have not found $LIMITS_FILE_NAME")
        return Optional.empty()
    }

    val limits = Properties()
    limits.load(Files.newInputStream(limitsFile.toPath()))

    val limitStr: String? = limits.getProperty(limitName)
    return if (limitStr == null) {
        project.logger.warn("Not found settings in ${limitsFile.name} for: type=$limitName")
        Optional.empty()
    } else Optional.of(Integer.valueOf(limitStr))
}