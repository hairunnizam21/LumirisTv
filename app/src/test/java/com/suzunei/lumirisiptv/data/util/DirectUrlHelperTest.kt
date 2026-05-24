package com.suzunei.lumirisiptv.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectUrlHelperTest {

    @Test
    fun delegatesDriveUrls() {
        val drive = "https://drive.google.com/file/d/ABC123/view?usp=sharing"
        val direct = DirectUrlHelper.convert(drive)
        assertEquals(
            "https://drive.usercontent.google.com/download?id=ABC123&export=download&confirm=t",
            direct,
        )
    }

    @Test
    fun convertsDropboxShareUrlToDirectContent() {
        val input = "https://www.dropbox.com/s/abc123def/video.mp4?dl=0"
        val converted = DirectUrlHelper.convert(input)
        assertTrue("Should rewrite host", converted.startsWith("https://dl.dropboxusercontent.com/s/"))
        assertTrue("Should carry raw=1", converted.contains("raw=1"))
        assertFalse("Should drop dl=0", converted.contains("dl=0"))
    }

    @Test
    fun convertsGitHubBlobToRaw() {
        val input = "https://github.com/user/repo/blob/main/path/to/video.mp4"
        val converted = DirectUrlHelper.convert(input)
        assertEquals(
            "https://raw.githubusercontent.com/user/repo/main/path/to/video.mp4",
            converted,
        )
    }

    @Test
    fun leavesRawGithubAlone() {
        val input = "https://raw.githubusercontent.com/user/repo/main/playlist.m3u"
        assertEquals(input, DirectUrlHelper.convert(input))
    }

    @Test
    fun leavesUnknownHostUnchanged() {
        val input = "https://example.com/stream/master.m3u8?token=abc"
        assertEquals(input, DirectUrlHelper.convert(input))
    }

    @Test
    fun detectsShareHosts() {
        assertTrue(DirectUrlHelper.isShareUrl("https://drive.google.com/file/d/X/view"))
        assertTrue(DirectUrlHelper.isShareUrl("https://www.dropbox.com/s/abc/video.mp4?dl=0"))
        assertTrue(DirectUrlHelper.isShareUrl("https://github.com/user/repo/blob/main/file"))
        assertTrue(DirectUrlHelper.isShareUrl("https://1drv.ms/v/s!abc"))
        assertFalse(DirectUrlHelper.isShareUrl("https://cdn.example.com/master.m3u8"))
        assertFalse(DirectUrlHelper.isShareUrl(""))
    }
}
