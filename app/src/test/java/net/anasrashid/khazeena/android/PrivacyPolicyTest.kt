package net.anasrashid.khazeena.android

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The privacy policy is published on the web and bundled in the app, and the two must be the
 * same words. A policy that says one thing on a website and another inside the app is worse than
 * having none: both are promises, and only one of them was read.
 *
 * Kept as two files rather than generated, because AGP will not take a Provider in a source set
 * and the Variant API plumbing to work around it is more machinery than this needs. The copy is
 * cheap; letting them drift is not.
 */
class PrivacyPolicyTest {

    private val root: File =
        generateSequence(File(System.getProperty("user.dir")!!)) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }

    @Test
    fun `the bundled policy is the published one, word for word`() {
        val published = File(root, "PRIVACY_khazeena.md")
        val bundled = File(root, "app/src/main/assets/privacy.md")
        assertTrue("PRIVACY_khazeena.md is missing", published.exists())
        assertTrue("app/src/main/assets/privacy.md is missing", bundled.exists())
        assertEquals(
            "PRIVACY_khazeena.md and the bundled copy differ — copy the former over the latter",
            published.readText(),
            bundled.readText(),
        )
    }

    @Test
    fun `the policy still names what the app actually talks to`() {
        // Not a spell check: if an endpoint is added or dropped in the code, the policy that
        // lists them has to move with it, and this is the reminder.
        val policy = File(root, "PRIVACY_khazeena.md").readText()
        listOf("cdn.jsdelivr.net", "api.ganjoor.net", "INTERNET").forEach {
            assertTrue("the policy no longer mentions $it", policy.contains(it))
        }
    }
}
