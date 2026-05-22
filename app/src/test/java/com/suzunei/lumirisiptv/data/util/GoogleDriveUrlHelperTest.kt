package com.suzunei.lumirisiptv.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveUrlHelperTest {

    @Test
    fun convertsFileSlashDShareUrl() {
        val input = "https://drive.google.com/file/d/1GyO97CfjeeE_bG-S9d5rl8FypcVYQvrA/view?usp=drivesdk"
        val expected = "https://drive.usercontent.google.com/download?id=1GyO97CfjeeE_bG-S9d5rl8FypcVYQvrA&export=download&confirm=t"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun convertsOpenIdShareUrl() {
        val input = "https://drive.google.com/open?id=ABCDEF"
        val expected = "https://drive.usercontent.google.com/download?id=ABCDEF&export=download&confirm=t"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun convertsPreviewUrl() {
        val input = "https://drive.google.com/file/d/XYZ_123-abc/preview"
        val expected = "https://drive.usercontent.google.com/download?id=XYZ_123-abc&export=download&confirm=t"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun convertsDocsUcUrl() {
        val input = "https://docs.google.com/uc?export=download&id=ABCDEF"
        val expected = "https://drive.usercontent.google.com/download?id=ABCDEF&export=download&confirm=t"
        assertEquals(expected, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun nonDriveUrlsPassThroughUnchanged() {
        val input = "https://example.com/video.m3u8?token=abc"
        assertEquals(input, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun alreadyConvertedUrlIsLeftAlone() {
        val input = "https://drive.usercontent.google.com/download?id=XYZ&export=download&confirm=t"
        assertEquals(input, GoogleDriveUrlHelper.convert(input))
    }

    @Test
    fun isDriveUrlDetectsAllHosts() {
        assertTrue(GoogleDriveUrlHelper.isDriveUrl("https://drive.google.com/file/d/X/view"))
        assertTrue(GoogleDriveUrlHelper.isDriveUrl("https://docs.google.com/uc?export=download&id=X"))
        assertTrue(GoogleDriveUrlHelper.isDriveUrl("https://drive.usercontent.google.com/download?id=X"))
        assertFalse(GoogleDriveUrlHelper.isDriveUrl("https://example.com/video.mp4"))
    }
}
