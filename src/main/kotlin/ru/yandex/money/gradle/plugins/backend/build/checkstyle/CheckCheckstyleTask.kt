package ru.yandex.money.gradle.plugins.backend.build.checkstyle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.backend.build.staticanalysis.StaticAnalysisProperties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Таска checkCheckstyle
 *
 * @author Andrey Mochalov
 * @since 22.04.2019
 */
open class CheckCheckstyleTask : DefaultTask() {

    @TaskAction
    fun checkCheckstyle() {
        val staticAnalysis = StaticAnalysisProperties.load(project)

        val checkstyleLimit = staticAnalysis?.checkstyle
        if (checkstyleLimit == null) {
            logger.warn("skipping check checkstyle")
            return
        }

        val reportsDir = project.extensions.getByType(CheckstyleExtension::class.java).reportsDir
        val checkStyleReport = project.file("$reportsDir/main.xml")
        if (!checkStyleReport.exists() || !checkStyleReport.isFile) {
            logger.warn("Have not found $reportsDir/main.xml, skipping check.")
            return
        }

        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        documentBuilderFactory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                false
        )
        documentBuilderFactory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        )

        val checkStyleReportDoc = documentBuilderFactory.newDocumentBuilder().parse(checkStyleReport)
        val errorsCount = checkStyleReportDoc.getElementsByTagName("error").length

        when {
            errorsCount > checkstyleLimit -> throw GradleException("Too much checkstyle errors: actual=$errorsCount," +
                    " limit=$checkstyleLimit")
            errorsCount < getCheckstyleLowerLimit(checkstyleLimit) -> updateIfLocalOrElseThrow(staticAnalysis, errorsCount)
            else -> logSuccess(errorsCount)
        }
    }

    private fun updateIfLocalOrElseThrow(staticAnalysis: StaticAnalysisProperties, errorsCount: Int) {
        if (!project.hasProperty("ci")) {
            staticAnalysis.checkstyle = errorsCount
            staticAnalysis.store()

            logSuccess(errorsCount)
        } else {
            throw GradleException("Сheckstyle limit is too high, must be $errorsCount. Decrease it in file static-analysis.properties.")
        }
    }

    private fun logSuccess(errorsCount: Int) {
        logger.lifecycle("Checkstyle check successfully passed with $errorsCount errors")
    }

    private fun getCheckstyleLowerLimit(limit: Int): Int {
        return limit * 95 / 100
    }
}
