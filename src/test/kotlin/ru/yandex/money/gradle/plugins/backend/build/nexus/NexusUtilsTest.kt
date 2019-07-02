package ru.yandex.money.gradle.plugins.backend.build.nexus

import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.matchesPattern
import org.testng.annotations.Test

class NexusUtilsTest {

    @Test
    fun should_return_resolve_major_version() {
        val version = NexusUtils.resolveVersion("ru.yandex.money.platform",
                "yamoney-libraries-dependencies", "+")
        assertTrue(version.isNotBlank())
    }

    @Test
    fun should_return_resolve_minor_version() {
        val version = NexusUtils.resolveVersion("ru.yandex.money.platform",
                "yamoney-libraries-dependencies", "3.+")
        assertThat(version, matchesPattern("3\\.\\d+\\.\\d+"))
    }

    @Test
    fun should_return_resolve_patch_version() {
        val version = NexusUtils.resolveVersion("ru.yandex.money.platform",
                "yamoney-libraries-dependencies", "3.1.+")
        assertThat(version, matchesPattern("3\\.1\\.\\d+"))
    }

    @Test
    fun should_return_resolve_full_version() {
        val version = NexusUtils.resolveVersion("ru.yandex.money.platform",
                "yamoney-libraries-dependencies", "3.1.15")
        assertThat(version, equalTo("3.1.15"))
    }
}