package com.example.newtv

import com.example.newtv.resolver.DeepLinkTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkTemplateTest {
    @Test
    fun replacesTokens() {
        val template = DeepLinkTemplate("intent://example/{contentId}/s{season}e{episode}")
        val result = template.render("abc", 2, 5)
        assertEquals("intent://example/abc/s2e5", result)
    }

    @Test
    fun leavesUnknownTokensAndReplacesRepeats() {
        val template = DeepLinkTemplate("https://h/{contentId}/{contentId}?x={other}")
        assertEquals("https://h/a/a?x={other}", template.render("a", 1, 1))
    }
}
