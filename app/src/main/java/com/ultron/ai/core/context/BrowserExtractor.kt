package com.ultron.ai.core.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Extracts and synthesizes webpage content for ULTRON AI Context Engine.
 * Fetches HTML, cleans tags/scripts, and prepares text chunks for LLM reasoning.
 */
class BrowserExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        private const val MAX_EXTRACT_CHARS = 12_000
        private val SCRIPT_REGEX = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE)
        private val STYLE_REGEX = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE)
        private val HTML_TAG_REGEX = Pattern.compile("<[^>]+>")
        private val TITLE_REGEX = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE)
    }

    /**
     * Extracts text content and title from [url].
     */
    suspend fun extractWebPage(url: String): ContextItem.WebContext = withContext(Dispatchers.IO) {
        val targetUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ContextItem.WebContext(
                        id = "web-${System.currentTimeMillis()}",
                        displayName = targetUrl,
                        url = targetUrl,
                        title = "Failed to load",
                        contentSummary = "HTTP error code: ${response.code}"
                    )
                }

                val html = response.body?.string().orEmpty()
                val title = extractTitle(html).ifBlank { targetUrl }
                val cleanText = sanitizeHtml(html)

                ContextItem.WebContext(
                    id = "web-${System.currentTimeMillis()}",
                    displayName = title,
                    url = targetUrl,
                    title = title,
                    contentSummary = cleanText.take(MAX_EXTRACT_CHARS)
                )
            }
        } catch (e: Exception) {
            ContextItem.WebContext(
                id = "web-${System.currentTimeMillis()}",
                displayName = targetUrl,
                url = targetUrl,
                title = "Connection Error",
                contentSummary = "Failed to reach webpage: ${e.message ?: "Unknown error"}"
            )
        }
    }

    private fun extractTitle(html: String): String {
        val matcher = TITLE_REGEX.matcher(html)
        return if (matcher.find()) {
            matcher.group(1)?.replace(HTML_TAG_REGEX.toRegex(), "")?.trim().orEmpty()
        } else {
            ""
        }
    }

    private fun sanitizeHtml(html: String): String {
        var text = SCRIPT_REGEX.matcher(html).replaceAll(" ")
        text = STYLE_REGEX.matcher(text).replaceAll(" ")
        text = HTML_TAG_REGEX.matcher(text).replaceAll(" ")
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
