package com.example.newtv.resolver

import com.example.newtv.data.ShowEntity

interface DeepLinkResolver {
    fun buildDeepLink(show: ShowEntity, season: Int, episode: Int): String?
}
