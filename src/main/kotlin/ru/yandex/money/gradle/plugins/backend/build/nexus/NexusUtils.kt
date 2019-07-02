package ru.yandex.money.gradle.plugins.backend.build.nexus

import org.apache.commons.io.IOUtils
import org.gradle.api.GradleException
import org.gradle.util.VersionNumber
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilderFactory
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Методы для работы с nexus
 *
 * @author horyukova
 * @since 28.06.2019
 */
object NexusUtils {
    /**
     * Метод возвращает последнюю версию библиотеки для версий вида "1.0.+", найденную в nexus
     *
     * @param depGroup группа артифакта
     * @param depName имя артифакта
     * @param version версия, содержащая '+', например "1.+"
     * @return последнюю версию библиотеки
     */
    fun resolveVersion(depGroup: String, depName: String, version: String): String {
        val path = depGroup.replace('.', '/')
        val versionsNodeList = getVersions(path, depName)

        return (0..versionsNodeList.length - 1)
                .map { versionsNodeList.item(it).firstChild.nodeValue }
                .filter { isMatchMajorVersion(it, version) }
                .maxWith(Comparator({ o1, o2 -> versionCompare(o1, o2) }))
                ?: throw GradleException("Not found version: dependencyName=$depName")
    }

    private fun versionCompare(o1: String, o2: String): Int {
        return VersionNumber.parse(o1).compareTo(VersionNumber.parse(o2))
    }

    private fun isMatchMajorVersion(repoVersion: String, version: String): Boolean {
        val majorVersion = version.replace(Regex("\\+.*"), "")
        return repoVersion.startsWith(majorVersion)
    }

    private fun getVersions(path: String, depName: String): NodeList {
        val url = "https://nexus.yamoney.ru/content/repositories/releases/$path/$depName/maven-metadata.xml"
        val content = IOUtils.toString(URL(url).openStream(), StandardCharsets.UTF_8.name())

        val dbFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = dbFactory.newDocumentBuilder()
        val doc = documentBuilder.parse(IOUtils.toInputStream(content))

        doc.documentElement.normalize()
        return doc.getElementsByTagName("version")
    }
}