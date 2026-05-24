package com.suzunei.lumirisiptv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashClearKeyInterceptorTest {

    private val interceptor = DashClearKeyInterceptor()

    @Test
    fun injectsClearKeyContentProtectionAfterCenc() {
        val xml = """
            <ContentProtection schemeIdUri="urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed" default_KID="912760c4-09eb-5aff-3e06-0422c502f410">
              <pssh>AAAA</pssh>
            </ContentProtection>
            <ContentProtection schemeIdUri="urn:mpeg:dash:mp4protection:2011" value="cenc" default_KID="912760c4-09eb-5aff-3e06-0422c502f410" />
        """.trimIndent()

        val rewritten = interceptor.rewrite(xml)

        assertTrue(
            "ClearKey scheme should be present",
            rewritten.contains("urn:uuid:e2719d58-a985-b3c9-781a-b030af78d30e"),
        )
        assertTrue(
            "default_KID should be preserved",
            rewritten.contains("default_KID=\"912760c4-09eb-5aff-3e06-0422c502f410\""),
        )
        assertTrue(
            "cenc:pssh element should be emitted",
            rewritten.contains("<cenc:pssh"),
        )
    }

    @Test
    fun isIdempotentWhenClearKeyAlreadyPresent() {
        val xml = """<ContentProtection schemeIdUri="urn:uuid:e2719d58-a985-b3c9-781a-b030af78d30e" />"""

        val rewritten = interceptor.rewrite(xml)

        assertEquals(xml, rewritten)
    }

    @Test
    fun doesNotChangeUnencryptedManifest() {
        val xml = """<MPD><Period><AdaptationSet /></Period></MPD>"""

        val rewritten = interceptor.rewrite(xml)

        assertEquals(xml, rewritten)
        assertFalse(rewritten.contains("ClearKey"))
    }

    @Test
    fun buildsValidPsshBoxForKid() {
        val pssh = interceptor.buildClearKeyPssh("912760c409eb5aff3e060422c502f410")
        assertNotNull(pssh)
        assertTrue("PSSH should be non-empty base64", pssh.isNotEmpty())
    }

    // --- KID normalization ---

    @Test
    fun normalizesCanonicalUuidKid() {
        val normalized = interceptor.normalizeKid("912760c4-09eb-5aff-3e06-0422c502f410")
        assertEquals("912760c409eb5aff3e060422c502f410", normalized)
    }

    @Test
    fun normalizesUndashedHexKid() {
        val normalized = interceptor.normalizeKid("912760c409eb5aff3e060422c502f410")
        assertEquals("912760c409eb5aff3e060422c502f410", normalized)
    }

    /** Regression for `IllegalArgumentException: KID must be 16 bytes, got 15`. */
    @Test
    fun padsLeadingZeroDroppedFromUuidSegment() {
        // Second segment is 3 hex chars instead of 4 (leading 0 was stripped).
        val normalized = interceptor.normalizeKid("912760c4-9eb-5aff-3e06-0422c502f410")
        assertEquals("912760c409eb5aff3e060422c502f410", normalized)
    }

    @Test
    fun padsLeadingZeroDroppedFromUndashedHex() {
        // 31 hex chars — pad with one leading zero to canonical 32.
        val normalized = interceptor.normalizeKid("12760c409eb5aff3e060422c502f410")
        assertEquals("012760c409eb5aff3e060422c502f410", normalized)
        assertEquals(32, normalized?.length)
    }

    @Test
    fun rejectsNonHexKid() {
        assertNull(interceptor.normalizeKid("not-a-hex-string-at-all-zzz-zzzz-zzzzzzzzzzzz"))
    }

    @Test
    fun rejectsOversizedSegment() {
        // Second segment is 5 chars — too long, can't be a UUID segment.
        assertNull(interceptor.normalizeKid("912760c4-09eb1-5aff-3e06-0422c502f410"))
    }

    /** End-to-end: a 15-byte (30-char) KID no longer crashes the interceptor. */
    @Test
    fun rewriteDoesNotCrashOnMalformedKid() {
        val xml = """<ContentProtection schemeIdUri="urn:mpeg:dash:mp4protection:2011" value="cenc" default_KID="912760c4-9eb-5aff-3e06-0422c502f410" />"""
        val rewritten = interceptor.rewrite(xml)
        // KID is repaired so ClearKey is still injected.
        assertTrue(rewritten.contains("urn:uuid:e2719d58-"))
    }

    @Test
    fun rewriteSkipsUnrepairableKidWithoutThrowing() {
        val xml = """<ContentProtection schemeIdUri="urn:mpeg:dash:mp4protection:2011" value="cenc" default_KID="not-hex-zzzz" />"""
        val rewritten = interceptor.rewrite(xml)
        assertFalse("ClearKey should NOT be injected for unrepairable KID", rewritten.contains("urn:uuid:e2719d58-"))
        assertEquals(xml, rewritten)
    }
}
