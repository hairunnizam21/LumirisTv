package com.suzunei.lumirisiptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3UParserTest {

    private val parser = M3UParser()

    @Test
    fun parsesCustomCategoryDelimiterFormat() {
        val raw = """
            === TV Malaysia ===

            TV1
            https://get.perfecttv.net/logo/tv1.png
            https://get.perfecttv.net/dash2.mpd?username=u&password=p&channel=tv1
            912760c409eb5aff3e060422c502f410:bea2d0f89fb3fbafa1fc9f34ba8734a6

            TV2
            https://get.perfecttv.net/logo/tv2.png
            https://get.perfecttv.net/dash2.mpd?username=u&password=p&channel=tv2
        """.trimIndent()

        val categories = parser.parse(raw)

        assertEquals(1, categories.size)
        assertEquals("TV Malaysia", categories[0].name)
        assertEquals(2, categories[0].channels.size)
        val tv1 = categories[0].channels[0]
        assertEquals("TV1", tv1.name)
        assertEquals("https://get.perfecttv.net/logo/tv1.png", tv1.logoUrl)
        assertTrue(tv1.streamUrl.endsWith("channel=tv1"))
        assertEquals(
            "912760c409eb5aff3e060422c502f410:bea2d0f89fb3fbafa1fc9f34ba8734a6",
            tv1.drmKey,
        )
        assertNull(categories[0].channels[1].drmKey)
    }

    @Test
    fun keepsChannelsEvenWhenLogoIsMissing() {
        val raw = """
            === Movie ===

            Mystery Movie Without Logo
            https://example.com/movie.m3u8

            With Logo
            https://example.com/cover.jpg
            https://example.com/movie.mp4
        """.trimIndent()

        val categories = parser.parse(raw)

        assertEquals(1, categories.size)
        assertEquals(2, categories[0].channels.size)
        val noLogo = categories[0].channels[0]
        assertEquals("Mystery Movie Without Logo", noLogo.name)
        assertNull(noLogo.logoUrl)
        assertEquals("https://example.com/movie.m3u8", noLogo.streamUrl)

        val withLogo = categories[0].channels[1]
        assertNotNull(withLogo.logoUrl)
        assertTrue(withLogo.streamUrl.endsWith(".mp4"))
    }

    @Test
    fun convertsGoogleDriveLinksToDirectStreams() {
        val raw = """
            === Anime ===

            Wistoria Episode 1
            https://m.media-amazon.com/image.jpg
            https://drive.google.com/file/d/1svDo4PB5D5WpMvwRzk0kzAi4A4U-KQ_L/view?usp=drivesdk
        """.trimIndent()

        val categories = parser.parse(raw)

        val ch = categories.single().channels.single()
        assertEquals(
            "https://docs.google.com/uc?export=download&id=1svDo4PB5D5WpMvwRzk0kzAi4A4U-KQ_L",
            ch.streamUrl,
        )
    }

    @Test
    fun switchesCategoriesAtEachDelimiter() {
        val raw = """
            === TV Malaysia ===

            TV1
            https://example.com/logo1.png
            https://example.com/tv1.mpd

            === Movie ===

            Comic 8
            https://example.com/cover.jpg
            https://drive.google.com/file/d/ABC123/view

            === Sukan ===

            Bein2 FHD
            https://example.com/bein2.png
            https://example.com/bein2.mpd
            efa6ff1acefa43048e8b7adc21d98871:5d0f448b52a92035e3763c4a60275933
        """.trimIndent()

        val categories = parser.parse(raw)

        assertEquals(listOf("TV Malaysia", "Movie", "Sukan"), categories.map { it.name })
        assertEquals(1, categories[0].channels.size)
        assertEquals(1, categories[1].channels.size)
        assertEquals(1, categories[2].channels.size)
        assertEquals("Bein2 FHD", categories[2].channels[0].name)
    }

    @Test
    fun parsesStandardExtinfWithGroupTitle() {
        val raw = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/logo.png" group-title="News",CNN
            https://example.com/cnn.m3u8
        """.trimIndent()

        val categories = parser.parse(raw)

        assertEquals(1, categories.size)
        assertEquals("News", categories[0].name)
        val cnn = categories[0].channels.single()
        assertEquals("CNN", cnn.name)
        assertEquals("https://example.com/logo.png", cnn.logoUrl)
        assertEquals("https://example.com/cnn.m3u8", cnn.streamUrl)
    }

    @Test
    fun ignoresEntriesWithoutStreamUrl() {
        val raw = """
            === Sukan ===

            Channel With Just A Title

            Real Channel
            https://example.com/real.m3u8
        """.trimIndent()

        val categories = parser.parse(raw)

        assertEquals(1, categories.size)
        assertEquals(1, categories[0].channels.size)
        assertEquals("Real Channel", categories[0].channels[0].name)
    }
}
