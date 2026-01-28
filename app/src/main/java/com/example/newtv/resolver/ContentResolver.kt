package com.example.newtv.resolver

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity

class ContentResolver(private val context: Context) : DeepLinkResolver {
    private val packageManager: PackageManager = context.packageManager

    override fun buildDeepLink(show: ShowEntity, season: Int, episode: Int): String? {
        if (!isProviderAvailable(show.provider)) {
            return null
        }
        return DeepLinkTemplate(show.deepLinkPattern)
            .render(show.contentId, season, episode)
    }

    fun resolveIntent(show: ShowEntity, season: Int, episode: Int): Intent? {
        val deepLink = buildDeepLink(show, season, episode) ?: return null
        val intent = try {
            if (deepLink.startsWith("intent://")) {
                Intent.parseUri(deepLink, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            }
        } catch (exception: Exception) {
            return null
        }
        intent.`package` = show.provider.packageName
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }

    fun buildMarketIntent(provider: Provider): Intent? {
        val marketUri = Uri.parse("market://details?id=${provider.packageName}")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${provider.packageName}")
        val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
        return if (marketIntent.resolveActivity(packageManager) != null) {
            marketIntent
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            if (webIntent.resolveActivity(packageManager) != null) webIntent else null
        }
    }

    fun isProviderAvailable(provider: Provider): Boolean {
        return packageManager.getLaunchIntentForPackage(provider.packageName) != null
    }
}
