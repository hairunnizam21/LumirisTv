package com.suzunei.lumirisiptv.data.util

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamTypeDetectorTest {

    @Test
    fun detectsDashByExtension() {
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/stream/manifest.mpd"),
        )
    }

    @Test
    fun detectsDashByQuery() {
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/stream?format=mpd&token=abc"),
        )
    }

    @Test
    fun detectsDashByPathKeyword() {
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/dash/manifest.mpd"),
        )
    }

    @Test
    fun detectsHlsByExtension() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/master.m3u8"),
        )
    }

    @Test
    fun detectsHlsWithQuerySuffix() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/master.m3u8?token=abc&exp=123"),
        )
    }

    @Test
    fun detectsHlsByQueryFormat() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/play?format=m3u8"),
        )
    }

    @Test
    fun detectsHlsByPathKeyword() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            StreamTypeDetector.detectMimeType("https://cdn.example.com/hls/livestream"),
        )
    }

    @Test
    fun detectsProgressive() {
        assertEquals(MimeTypes.VIDEO_MP4, StreamTypeDetector.detectMimeType("https://x.com/v.mp4"))
        assertEquals(MimeTypes.VIDEO_MATROSKA, StreamTypeDetector.detectMimeType("https://x.com/v.mkv"))
        assertEquals(MimeTypes.VIDEO_WEBM, StreamTypeDetector.detectMimeType("https://x.com/v.webm"))
        assertEquals(MimeTypes.VIDEO_MP2T, StreamTypeDetector.detectMimeType("https://x.com/seg.ts"))
    }

    @Test
    fun returnsNullForDriveUrlsSoMedia3CanSniff() {
        assertNull(StreamTypeDetector.detectMimeType("https://drive.google.com/file/d/ABC/view"))
    }

    @Test
    fun returnsNullForUnknownUrls() {
        assertNull(StreamTypeDetector.detectMimeType("https://cdn.example.com/play"))
        assertNull(StreamTypeDetector.detectMimeType(""))
    }
}
