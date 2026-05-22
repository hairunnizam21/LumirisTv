package com.suzunei.lumirisiptv.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveUrlHelperTest {

    @Test
    fun convertsFileSlashDShareUrl() {
        val input = "https://drive.google.com/file/d/1GyO97CfjeeE_bG-S9d5rl8FypcVYQvrA/view?usp=drivesdk"
        val expected = "https://docs.google.com/uc?export=download&id=1GyO97CfjeeE_bG-S9d5rl8FypcVYQvrA"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun convertsOpenIdShareUrl() {
        val input = "https://drive.google.com/open?id=ABCDEF"
        val expected = "https://docs.google.com/uc?export=download&id=ABCDEF"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun convertsPreviewUrl() {
        val input = "https://drive.google.com/file/d/XYZ_123-abc/preview"
        val expected = "https://docs.google.com/uc?export=download&id=XYZ_123-abc"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun nonDriveUrlsPassThroughUnchanged() {
        val input = "https://example.com/video.m3u8?token=abc"
        assertEquals(input, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun isDriveUrlDetectsBothHosts() {
        assertTrue(GoogleDriveUrlHelper.isDriveUrl("https://drive.google.com/file/d/X/view"))
        assertTrue(GoogleDriveUrlHelper.isDriveUrl("https://docs.google.com/uc?export=download&id=X"))
        assertFalse(GoogleDriveUrlHelper.isDriveUrl("https://example.com/video.mp4"))
    }
}
