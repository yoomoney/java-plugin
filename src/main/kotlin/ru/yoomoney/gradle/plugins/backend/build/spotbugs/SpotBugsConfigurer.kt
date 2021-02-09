package ru.yoomoney.gradle.plugins.backend.build.spotbugs

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsReport
import com.github.spotbugs.snom.SpotBugsTask
import org.apache.commons.io.IOUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.file.TmpDirTemporaryFileProvider
import org.gradle.api.internal.resources.StringBackedTextResource
import org.gradle.api.plugins.JavaPluginConvention
import ru.yoomoney.gradle.plugins.backend.build.JavaExtension
import ru.yoomoney.gradle.plugins.backend.build.git.GitManager
import ru.yoomoney.gradle.plugins.backend.build.staticanalysis.StaticAnalysisProperties
import java.lang.Integer.max
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Настраивает статический анализ SpotBugs
 *
 * @author Valerii Zhirnov
 * @since 19.04.2019
 */
class SpotBugsConfigurer {

    fun init(target: Project) {
        val gitManager = GitManager(target)

        val extension = target.extensions.getByType(SpotBugsExtension::class.java)
        extension.toolVersion.set("4.0.1")
        extension.reportsDir.set(target.file(target.buildDir.resolve("spotbugsReports")))
        extension.effort.set(Effort.DEFAULT)
        extension.reportLevel.set(Confidence.MEDIUM)
        extension.excludeFilter.set(StringBackedTextResource(TmpDirTemporaryFileProvider(), spotbugsExclude()).asFile())
        extension.ignoreFailures.set(true)

        with(target.tasks.create("spotbugsMain", SpotBugsTask::class.java)) {
            val mainSourceSet = target.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName("main")
            reports.create("xml")
            maxHeapSize.set("3g")
            sourceDirs.plus(mainSourceSet.allSource.srcDirs)
            classDirs = mainSourceSet.output
            auxClassPaths = mainSourceSet.compileClasspath

            onlyIf { spotbugsEnabled(target, gitManager) }
        }

        target.tasks.getByName("build").dependsOn("spotbugsMain")
        applyCheckTask(target)
        target.tasks.getByName("spotbugsMain").finalizedBy("checkFindBugsReport")
        target.dependencies.add(
            "spotbugsPlugins",
            "com.mebigfatguy.sb-contrib:sb-contrib:7.4.7"
        )
        target.dependencies.add(
            "spotbugsPlugins",
            "com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.1"
        )
    }

    private fun applyCheckTask(target: Project) {
        target.tasks.create("checkFindBugsReport").doLast {
            target.logger.lifecycle("Run checkFindBugsReport")
            val staticAnalysis = StaticAnalysisProperties.load(target)

            val findbugsLimit = staticAnalysis?.getProperty(StaticAnalysisProperties.FINDBUGS_KEY)
            if (findbugsLimit == null) {
                target.logger.warn("findbugs limit not found, skipping check")
                return@doLast
            }
            val spotBugsTask = target.tasks.getByName("spotbugsMain") as SpotBugsTask

            val xmlReport = if (spotBugsTask.reports.findByName("xml") != null) {
                spotBugsTask.reports.findByName("xml")
            } else {
                spotBugsTask.reports.create("xml")
            }
            // если в проекте нет исходников, то отчет создан не будет. Пропускает такие проекты
            if (!xmlReport?.destination?.exists()!!) {
                target.logger.lifecycle("SpotBugs report not found: ${xmlReport.destination}. SpotBugs skipped.")
                return@doLast
            }

            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlReport.destination)
            val bugsFound = document.getElementsByTagName("BugInstance").length

            when {
                bugsFound > findbugsLimit -> throw GradleException("Too much SpotBugs errors: actual=$bugsFound, " +
                    "limit=$findbugsLimit. See the report at: ${xmlReport.destination}")
                bugsFound < max(0, findbugsLimit - 10) -> updateIfLocalOrElseThrow(target, staticAnalysis, bugsFound,
                    findbugsLimit, xmlReport)
                else -> logSuccess(target, bugsFound, findbugsLimit, xmlReport)
            }
        }
    }

    private fun spotbugsEnabled(target: Project, gitManager: GitManager): Boolean {
        if (!gitManager.isDevelopmentBranch()) {
            target.logger.warn("SpotBugs check is enabled on feature/ and hotfix/ and bugfix/ branches. Skipping.")
            return false
        }
        val module = target.extensions.getByType(JavaExtension::class.java)
        return module.spotbugsEnabled
    }

    private fun spotbugsExclude(): String {
        val inputStream = this.javaClass.getResourceAsStream("/findbugs-exclude.xml")
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8)
    }

    private fun updateIfLocalOrElseThrow(target: Project, staticAnalysis: StaticAnalysisProperties, bugsFound: Int, bugsLimit: Int, xmlReport: SpotBugsReport) {
        if (!target.hasProperty("ci")) {
            staticAnalysis.setProperty(StaticAnalysisProperties.FINDBUGS_KEY, bugsFound)
            staticAnalysis.store()

            logSuccess(target, bugsFound, bugsLimit, xmlReport)
        } else {
            throw GradleException("SpotBugs limit is too high, " +
                "must be $bugsFound. Decrease it in file static-analysis.properties.")
        }
    }

    private fun logSuccess(target: Project, bugsFound: Int, bugsLimit: Int, xmlReport: SpotBugsReport) {
        target.logger.lifecycle("SpotBugs successfully passed with $bugsFound (limit=$bugsLimit) errors." +
            " See the report at: ${xmlReport.destination}")
    }
}
