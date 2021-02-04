package ru.yoomoney.gradle.plugins.backend.build.test

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Задача для перезаписи system-out в отчетах компонентных тестов с разбиением по TraceID.
 *
 * @author Dmitry Komarov
 * @since 14.02.2020
 */
open class OverwriteTestReportsTask : DefaultTask() {

    companion object {
        /**
         * Директория с логами, разбитыми по TraceID.
         */
        private const val TRACE_ID_LOGS_DIRECTORY = "tmp/logs/test"

        private const val TRACE_ID_LOG_FILE_PREFIX = "LOGS-"
        private const val TRACE_ID_LOG_FILE_POSTFIX = ".xml"

        private const val REPORT_FILE_PREFIX = "TEST-"
        private const val REPORT_FILE_POSTFIX = ".xml"

        private val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        private val transformerFactory = TransformerFactory.newInstance()

        private const val TEST_CASE_TAG_NAME = "testcase"
        private const val TEST_CASE_NAME_ATTRIBUTE_NAME = "name"
        private const val TEST_LOG_TAG_NAME = "testlog"
        private const val TEST_LOG_NAME_ATTRIBUTE_NAME = "name"

        private const val SYSTEM_OUT_TAG_NAME = "system-out"

        private val XML_INVALID_CHARS = "&#(x?)([A-Fa-f0-9]+);".toRegex()
    }

    @Input
    lateinit var xmlReportsPath: String

    @TaskAction
    fun overwriteTestReports() {
        val traceIdLogs = collectTraceIdLogs()
        if (traceIdLogs.isEmpty()) {
            return
        }
        val reportsFileTree = project.fileTree(xmlReportsPath)
        reportsFileTree
                .filter { it.name.startsWith(REPORT_FILE_PREFIX) }
                .forEach { overwriteReportFile(it, traceIdLogs) }
    }

    private fun collectTraceIdLogs(): Map<String, File> {
        val traceIdLogs = mutableMapOf<String, File>()
        val logsFileTree = project.fileTree("${project.buildDir}/$TRACE_ID_LOGS_DIRECTORY")
        logsFileTree.forEach {
            val fullyQualifiedClassName = it.name.substring(
                    TRACE_ID_LOG_FILE_PREFIX.length,
                    it.name.length - TRACE_ID_LOG_FILE_POSTFIX.length
            )
            traceIdLogs[fullyQualifiedClassName] = it
        }
        return traceIdLogs
    }

    private fun overwriteReportFile(reportFile: File, traceIdLogs: Map<String, File>) {
        val fullyQualifiedClassName = reportFile.name.substring(
                REPORT_FILE_PREFIX.length,
                reportFile.name.length - REPORT_FILE_POSTFIX.length
        )
        if (fullyQualifiedClassName !in traceIdLogs) {
            logger.debug("Unknown report file to overwrite: reportFile=$reportFile")
            return
        }
        val traceIdLogFile = traceIdLogs[fullyQualifiedClassName]
                ?: error("TraceId log file is expected: className=$fullyQualifiedClassName")

        val reportXmlDocument = parseXmlDocument(reportFile)
        val traceIdLogsXmlDocument = parseXmlDocument(traceIdLogFile)
        if (overwriteReport(reportXmlDocument, traceIdLogsXmlDocument)) {
            val transformer = transformerFactory.newTransformer()
            val output = StreamResult(reportFile)
            val source = DOMSource(reportXmlDocument)
            transformer.transform(source, output)
        }
    }

    private fun overwriteReport(reportXmlDocument: Document, traceIdLogsXmlDocument: Document): Boolean {
        val testCases = reportXmlDocument.getElementsByTagName(TEST_CASE_TAG_NAME)
        val testLogs = traceIdLogsXmlDocument.getElementsByTagName(TEST_LOG_TAG_NAME)

        var needToOverwriteFile = false
        for (i in 0 until testCases.length) {
            val testCase = testCases.item(i)
            val testCaseName = testCase.attributes.getNamedItem(TEST_CASE_NAME_ATTRIBUTE_NAME).textContent

            val testLog = findTestLog(testLogs, testCaseName)
            if (testLog != null) {
                val logs = testLog.textContent
                needToOverwriteFile = overwriteTestCase(testCase, logs)
            }
        }
        return needToOverwriteFile
    }

    private fun overwriteTestCase(testCase: Node, logs: String): Boolean {
        val children = testCase.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeName == SYSTEM_OUT_TAG_NAME) {
                val document = testCase.ownerDocument
                val cdata = document.createCDATASection(logs)
                child.textContent = null
                child.appendChild(cdata)
                return true
            }
        }
        logger.debug("'$SYSTEM_OUT_TAG_NAME' tag not found for given test case")
        return false
    }

    private fun findTestLog(testLogs: NodeList, name: String): Node? {
        for (i in 0 until testLogs.length) {
            val testLog = testLogs.item(i)
            val testLogName = testLog.attributes.getNamedItem(TEST_LOG_NAME_ATTRIBUTE_NAME).textContent
            if (testLogName == name) {
                return testLog
            }
        }
        return null
    }

    private fun parseXmlDocument(file: File): Document {
        val content = file.readText().replace(XML_INVALID_CHARS, "")
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val inputSource = InputSource(StringReader(content))
        return documentBuilder.parse(inputSource)
    }
}
