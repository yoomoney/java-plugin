package ru.yandex.money.gradle.plugins.backend.build.coverage

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Конфигурация jacoco
 *
 * @author Andrey Mochalov
 * @since 23.04.2019
 */
class CoverageConfigurer {

    fun init(project: Project) {
        val jacocoPluginExtension = project.extensions.getByType(JacocoPluginExtension::class.java)
        jacocoPluginExtension.toolVersion = "0.8.3"

        val jacocoTestReportTask = project.tasks.findByName("jacocoTestReport") as JacocoReport
        jacocoTestReportTask.reports.xml.isEnabled = true
        jacocoTestReportTask.reports.html.isEnabled = true

        val jacocoAggReportTask = configureJacocoAggReportTask(project)
        val checkCoverageTask = configureCheckCoverageTask(project)

        jacocoAggReportTask.dependsOn("check", "slowTest")
        checkCoverageTask.dependsOn("jacocoAggReport")
    }

    private fun configureJacocoAggReportTask(project: Project): JacocoReport {
        val jacocoAggReportTask = project.tasks.create("jacocoAggReport", JacocoReport::class.java)
        val sourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
        jacocoAggReportTask.sourceDirectories = project.files(sourceSets.getByName("main").allSource.srcDirs)
        jacocoAggReportTask.classDirectories = project.files(sourceSets.getByName("main").output)
        jacocoAggReportTask.executionData = project.files()
        jacocoAggReportTask.reports.xml.isEnabled = true
        jacocoAggReportTask.reports.html.isEnabled = true
        jacocoAggReportTask.doFirst {
            val reportDir = project.file("${project.buildDir}/jacoco")
            if (reportDir.exists()) {
                jacocoAggReportTask.executionData = project.files(reportDir.listFiles())
            }
        }
        jacocoAggReportTask.setOnlyIf {
            true
        }
        return jacocoAggReportTask
    }

    @Suppress("ReturnCount", "ComplexMethod")
    private fun configureCheckCoverageTask(project: Project): Task {
        val checkCoverageTask = project.tasks.create("checkCoverage") {
            it.description = "Check Coverage"
        }
        return checkCoverageTask.doLast {
            val coveragePropertiesFile = project.file("coverage.properties")
            if (!coveragePropertiesFile.exists() || !coveragePropertiesFile.isFile) {
                project.logger.warn("Have not found coverage.properties, skipping check.")
                return@doLast
            }

            val jacocoTestReport = project.file("${project.buildDir}/reports/jacoco/jacocoAggReport/" +
                    "jacocoAggReport.xml")
            if (!jacocoTestReport.exists() || !jacocoTestReport.isFile) {
                project.logger.warn("Have not found jacocoAggReport.xml, skipping check.")
                return@doLast
            }

            val coverageLimits = Properties().apply {
                FileInputStream(coveragePropertiesFile).use {
                    load(it)
                }
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
            val actualCoverageData = documentBuilderFactory.newDocumentBuilder().parse(jacocoTestReport)
            var isLimitsCheckPass = true
            var errorMessages = ""
            var currentCoverageInfo = "Current coverage:"
            var newCoverage = "\n"
            val nodes = actualCoverageData.documentElement.childNodes
            for (i in 0 until nodes.length) {
                if (nodes.item(i).nodeName == "counter") {
                    val counter = nodes.item(i)
                    val type = counter.attributes.getNamedItem("type").textContent.toLowerCase()
                    if (type == "line" || "complexity" == type) {
                        continue
                    }
                    val coverageLimit = coverageLimits.getProperty(type)
                            ?: throw GradleException("Not found settings in coverage.properties for: type=$type")
                    val limit = coverageLimit.toDouble().toInt()
                    val covered = counter.attributes.getNamedItem("covered").textContent.toDouble()
                    val coveragePercent = (100 * covered / (counter.attributes.getNamedItem("missed")
                            .textContent.toDouble() + covered)).toInt()
                    val maxLimitWithoutIncrease = limit + 3
                    currentCoverageInfo += if (coveragePercent > maxLimitWithoutIncrease) {
                        if (!project.hasProperty("ci")) {
                            coverageLimits.setProperty(type, coveragePercent.toString())
                            coverageLimits.apply {
                                FileOutputStream(coveragePropertiesFile).use {
                                    store(it, null)
                                }
                            }
                            project.logger.info("Coverage increased for type=$type, setting limit to $coveragePercent")
                            "\n[$type]: actual=$coveragePercent, limit=$coveragePercent"
                        } else {
                            isLimitsCheckPass = false
                            errorMessages += "\nGreat! Coverage gone up, increase it to $coveragePercent in " +
                                    "coverage.properties and you're good to go: type=$type, " +
                                    "actual=$coveragePercent, limit=$limit"
                            "\n[$type]: actual=$coveragePercent, limit=$limit"
                        }
                    } else {
                        "\n[$type]: actual=$coveragePercent, limit=$limit"
                    }
                    newCoverage += "$type=$coveragePercent\n"
                    if (coveragePercent < limit) {
                        isLimitsCheckPass = false
                        errorMessages += "\nNeed more tests! Not enough coverage for: type=$type, " +
                                "actual=$coveragePercent, limit=$limit"
                    }
                }
            }

            project.logger.lifecycle(currentCoverageInfo)
            if (isLimitsCheckPass) {
                project.logger.info("Coverage check successfully passed")
            } else {
                project.logger.info("New coverage.properties: $newCoverage")
                throw GradleException("Coverage limit failure: $errorMessages")
            }
        }
    }
}
