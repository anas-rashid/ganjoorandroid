package com.ganjoor.android

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The download the README offers must be the release the build actually produces.
 *
 * A README that advertises last version is worse than one that says nothing: someone follows the
 * link, installs an old build, and has no way to know. tools/update_release_docs.py keeps these in
 * step; this is what makes forgetting to run it a failed build rather than a quiet wrong answer.
 */
class ReleaseDocsTest {

    private val root: File =
        generateSequence(File(System.getProperty("user.dir")!!)) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }

    private fun read(path: String) = File(root, path).readText()

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }

    private val declaredVersion: String by lazy {
        Regex("versionName\\s*=\\s*\"([^\"]+)\"")
            .find(read("app/build.gradle.kts"))!!
            .groupValues[1]
    }

    @Test
    fun `the README offers the version the build declares`() {
        val offered = Regex("### . \\[Download ganjoor-([^\\]]+)\\.apk]")
            .find(read("README.md"))
            ?.groupValues?.get(1)
        assertEquals(
            "README.md offers a different version than app/build.gradle.kts declares — " +
                "run tools/update_release_docs.py",
            declaredVersion,
            offered,
        )
    }

    @Test
    fun `the APK the README links to is archived`() {
        val apk = File(root, "releases/ganjoor-" + declaredVersion + ".apk")
        assertTrue(apk.name + " is linked from README.md but not in releases/", apk.exists())
    }

    @Test
    fun `the README short checksum matches the archived APK`() {
        val short = Regex("`sha256 ([0-9a-f]+)…([0-9a-f]+)`")
            .find(read("README.md"))!!
            .groupValues
        val full = sha256(File(root, "releases/ganjoor-" + declaredVersion + ".apk"))
        assertTrue(
            "README.md's checksum does not match releases/ganjoor-" + declaredVersion + ".apk",
            full.startsWith(short[1]) && full.endsWith(short[2]),
        )
    }

    @Test
    fun `every checksum in releases matches the APK beside it`() {
        val listed = Regex("^([0-9a-f]{64})  (ganjoor-.+\\.apk)$", RegexOption.MULTILINE)
            .findAll(read("releases/README.md"))
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
        assertTrue("no checksums found in releases/README.md", listed.isNotEmpty())
        listed.forEach { (digest, name) ->
            val apk = File(root, "releases/" + name)
            assertTrue(name + " is listed in releases/README.md but missing", apk.exists())
            assertEquals(name + " does not match its listed checksum", digest, sha256(apk))
        }
    }
}
