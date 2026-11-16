package com.ultron.ai.core

/**
 * Works out where you'd go to do a thing.
 *
 * "text text text" is only useful as a location reminder if we know
 * milk means a shop — so each errand is mapped to the OpenStreetMap tags that
 * describe the right kind of place. Kept pure and data-driven so the mapping
 * can be read, tested and extended without touching any networking.
 */
object ErrandPlaces {

    /** An OSM tag filter, e.g. `shop=supermarket`. */
    data class PlaceType(val key: String, val value: String) {
        /** Overpass query fragment for this type within [radius] of a point. */
        fun overpassClause(radius: Int, lat: Double, lon: Double): String =
            """node["$key"="$value"](around:$radius,$lat,$lon);"""
    }

    private val GROCERY = listOf(
        PlaceType("shop", "supermarket"),
        PlaceType("shop", "convenience"),
        PlaceType("shop", "grocery")
    )
    private val PHARMACY = listOf(PlaceType("amenity", "pharmacy"))
    private val BANK = listOf(PlaceType("amenity", "bank"), PlaceType("amenity", "atm"))
    private val CAFE = listOf(PlaceType("amenity", "cafe"))
    private val BAKERY = listOf(PlaceType("shop", "bakery")) + GROCERY
    private val POST = listOf(PlaceType("amenity", "post_office"))
    private val FUEL = listOf(PlaceType("amenity", "fuel"))
    private val HARDWARE = listOf(PlaceType("shop", "hardware"), PlaceType("shop", "doityourself"))

    /**
     * Keyword → place types. Matched on word stems so Russian cases don't
     * matter ("text" / "text" / "text").
     */
    private val KEYWORDS: List<Pair<List<String>, List<PlaceType>>> = listOf(
        listOf(
            "groceries", "grocery", "milk", "bread", "food", "supermarket", "provisions", "vegetables", "fruits"
        ) to GROCERY,
        listOf(
            "pharmacy", "medicine", "tablets", "pills", "medical", "chemist"
        ) to PHARMACY,
        listOf(
            "bank", "atm", "cash", "money", "withdraw"
        ) to BANK,
        listOf("coffee", "cafe", "tea") to CAFE,
        listOf("bakery", "cake", "bread", "buns", "pastry") to BAKERY,
        listOf("post", "parcel", "courier", "package") to POST,
        listOf("fuel", "petrol", "gas station", "diesel") to FUEL,
        listOf("hardware", "tools", "screws", "paint") to HARDWARE
    )

    /**
     * Place types worth checking for [errand], or an empty list when the
     * errand isn't location-shaped at all. Empty means "don't turn this into
     * a location reminder" — guessing would produce nagging in random shops.
     */
    fun placeTypesFor(errand: String): List<PlaceType> {
        val text = errand.lowercase()
        val matched = KEYWORDS
            .filter { (keywords, _) -> keywords.any { text.contains(it) } }
            .flatMap { it.second }
            .distinct()

        if (matched.isEmpty() && looksLikeShopping(text)) return GROCERY
        return matched
    }

    private fun looksLikeShopping(text: String): Boolean =
        listOf("buy", "pick up", "get", "purchase", "vaangu")
            .any { text.contains(it) }

    /** True when this errand is worth watching for nearby places. */
    fun isLocationWorthy(errand: String): Boolean = placeTypesFor(errand).isNotEmpty()

    /**
     * The sentence Ultron says when you're near somewhere useful.
     */
    fun nearbyPrompt(errand: String, nearestMeters: Int, placeCount: Int, russian: Boolean = false): String =
        "You wanted to get $errand. " +
            if (placeCount > 1) "There are places nearby, the closest is $nearestMeters metres away."
            else "There's one nearby, $nearestMeters metres away."

    /** Straight-line distance in metres — good enough to rank nearby shops. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return (earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toInt()
    }
}
