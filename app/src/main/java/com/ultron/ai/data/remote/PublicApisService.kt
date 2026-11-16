package com.ultron.ai.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Service to search and retrieve free public APIs from github.com/public-apis/public-apis.
 *
 * Provides live online lookup with an embedded offline catalog fallback covering
 * major developer API categories.
 */
class PublicApisService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    data class PublicApiEntry(
        val title: String,
        val description: String,
        val auth: String,
        val https: Boolean,
        val cors: String,
        val link: String,
        val category: String
    )

    companion object {
        private const val TAG = "PublicApis"
        private const val RAW_README_URL =
            "https://raw.githubusercontent.com/public-apis/public-apis/master/README.md"

        private val CURATED_CATALOG = listOf(
            PublicApiEntry("Open-Meteo", "Free weather forecast API for non-commercial use with no API key", "No", true, "yes", "https://open-meteo.com", "Weather"),
            PublicApiEntry("CoinGecko", "Cryptocurrency data API providing prices, volumes, market caps and exchanges", "No", true, "yes", "https://www.coingecko.com/api", "Cryptocurrency"),
            PublicApiEntry("PokeAPI", "RESTful Pokémon API providing data on all Pokémon games and characters", "No", true, "yes", "https://pokeapi.co", "Games & Comics"),
            PublicApiEntry("OpenLibrary", "Books, book covers, search and bibliographic database", "No", true, "yes", "https://openlibrary.org/developers/api", "Books"),
            PublicApiEntry("NASA Open APIs", "NASA data including Astronomy Picture of the Day, Mars Rover Photos, asteroids", "apiKey", true, "yes", "https://api.nasa.gov", "Science & Math"),
            PublicApiEntry("REST Countries", "Information about world countries including borders, currencies, capitals", "No", true, "yes", "https://restcountries.com", "Geocoding"),
            PublicApiEntry("Chuck Norris Jokes", "Handcrafted Chuck Norris jokes delivered via JSON REST API", "No", true, "yes", "https://api.chucknorris.io", "Entertainment"),
            PublicApiEntry("GitHub REST API", "Access public repositories, users, issues, commits, and pulls", "OAuth / Token", true, "yes", "https://docs.github.com/en/rest", "Development"),
            PublicApiEntry("HackerNews Firebase", "Official Hacker News news, comments, stories, and jobs API", "No", true, "yes", "https://github.com/HackerNews/API", "News"),
            PublicApiEntry("ExchangeRate-API", "Free currency exchange rates with support for 160+ currencies", "apiKey", true, "yes", "https://www.exchangerate-api.com", "Finance"),
            PublicApiEntry("The Dog API", "A public API for dogs: breeds, sub-breeds, and images", "apiKey", true, "yes", "https://thedogapi.com", "Animals"),
            PublicApiEntry("The Cat API", "Pictures and info about cats as a service", "apiKey", true, "yes", "https://thecatapi.com", "Animals"),
            PublicApiEntry("Jikan (MyAnimeList)", "Unofficial open-source MyAnimeList REST API for anime & manga", "No", true, "yes", "https://jikan.moe", "Anime"),
            PublicApiEntry("OpenFoodFacts", "Database of food products with ingredients, allergens, nutrition facts", "No", true, "yes", "https://world.openfoodfacts.org/data", "Food & Drink"),
            PublicApiEntry("Universities List", "List of universities around the world by country", "No", true, "yes", "https://github.com/Hipo/university-domains-list", "Education"),
            PublicApiEntry("Nominatim OpenStreetMap", "Geocoding and reverse geocoding via OpenStreetMap", "No", true, "yes", "https://nominatim.org", "Geocoding")
        )
    }

    /**
     * Search public APIs matching [query] and optional [category].
     */
    suspend fun search(query: String, category: String? = null): List<PublicApiEntry> =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            val cat = category?.trim()?.lowercase()

            val liveResults = fetchLive(q, cat)
            if (liveResults.isNotEmpty()) {
                return@withContext liveResults
            }

            // Fallback to curated catalog
            CURATED_CATALOG.filter { entry ->
                val matchesQuery = q.isEmpty() ||
                        entry.title.lowercase().contains(q) ||
                        entry.description.lowercase().contains(q) ||
                        entry.category.lowercase().contains(q)
                val matchesCat = cat.isNullOrEmpty() || entry.category.lowercase().contains(cat)
                matchesQuery && matchesCat
            }
        }

    private fun fetchLive(query: String, category: String?): List<PublicApiEntry> {
        return try {
            val request = Request.Builder()
                .url(RAW_README_URL)
                .header("User-Agent", "Ultron-AI-PublicApis/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                parseMarkdownTable(body, query, category)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live public-apis README: ${e.message}")
            emptyList()
        }
    }

    private fun parseMarkdownTable(markdown: String, query: String, categoryFilter: String?): List<PublicApiEntry> {
        val results = mutableListOf<PublicApiEntry>()
        var currentCategory = "General"

        for (line in markdown.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("### ")) {
                currentCategory = trimmed.removePrefix("### ").trim()
                continue
            }

            if (trimmed.startsWith("|") && !trimmed.startsWith("| API") && !trimmed.startsWith("|:---")) {
                val parts = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size >= 5) {
                    val rawTitle = parts[0]
                    val linkMatch = Regex("""\[(.*?)\]\((.*?)\)""").find(rawTitle)
                    val title = linkMatch?.groupValues?.get(1) ?: rawTitle
                    val link = linkMatch?.groupValues?.get(2) ?: ""
                    val description = parts[1]
                    val auth = parts[2]
                    val https = parts[3].equals("Yes", ignoreCase = true)
                    val cors = parts.getOrNull(4) ?: "unknown"

                    val matchesQuery = query.isEmpty() ||
                            title.lowercase().contains(query) ||
                            description.lowercase().contains(query) ||
                            currentCategory.lowercase().contains(query)
                    val matchesCategory = categoryFilter.isNullOrEmpty() ||
                            currentCategory.lowercase().contains(categoryFilter)

                    if (matchesQuery && matchesCategory) {
                        results.add(
                            PublicApiEntry(
                                title = title,
                                description = description,
                                auth = auth,
                                https = https,
                                cors = cors,
                                link = link,
                                category = currentCategory
                            )
                        )
                        if (results.size >= 10) break
                    }
                }
            }
        }
        return results
    }

    fun formatSummary(results: List<PublicApiEntry>, query: String): String {
        if (results.isEmpty()) {
            return "No public APIs found matching '$query' in github.com/public-apis/public-apis."
        }
        val builder = StringBuilder("Found ${results.size} public API(s) for '$query':\n")
        results.take(5).forEachIndexed { index, api ->
            builder.append("${index + 1}. ${api.title} [${api.category}]\n")
            builder.append("   • Description: ${api.description}\n")
            builder.append("   • Auth: ${api.auth} | HTTPS: ${if (api.https) "Yes" else "No"} | CORS: ${api.cors}\n")
            if (api.link.isNotEmpty()) {
                builder.append("   • Link: ${api.link}\n")
            }
        }
        return builder.toString().trimEnd()
    }
}
