// ANZ -->
package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.anilist.dto.ALPageResponse
import eu.kanade.tachiyomi.data.track.anilist.dto.AniListMedia
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import kotlin.random.Random

class AniListRepository(
    private val network: NetworkHelper = Injekt.get(),
    private val json: Json = Injekt.get(),
) {
    private val graphQlUrl = "https://graphql.anilist.co"
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val mediaFragment = """
        id
        title {
            english
            romaji
            native
            userPreferred
        }
        description(asHtml: false)
        bannerImage
        coverImage {
            extraLarge
            large
            medium
        }
        genres
        averageScore
        format
        status
        episodes
        seasonYear
        season
    """.trimIndent()

    suspend fun getTrending(): List<AniListMedia> {
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, sort: TRENDING_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getPopular(): List<AniListMedia> {
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, sort: POPULARITY_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getTopRated(): List<AniListMedia> {
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, sort: SCORE_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getThisSeason(): List<AniListMedia> {
        val now = LocalDate.now()
        val year = now.year
        val season = when (now.monthValue) {
            1, 2, 3 -> "WINTER"
            4, 5, 6 -> "SPRING"
            7, 8, 9 -> "SUMMER"
            else -> "FALL"
        }
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, season: $season, seasonYear: $year, sort: POPULARITY_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getMovies(): List<AniListMedia> {
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, format: MOVIE, sort: POPULARITY_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getByGenre(genre: String): List<AniListMedia> {
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, genre: "$genre", sort: POPULARITY_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun search(queryText: String): List<AniListMedia> {
        if (queryText.isBlank()) return emptyList()
        val escaped = queryText.replace("\"", "\\\"")
        val query = """
            query {
                Page(page: 1, perPage: 20) {
                    media(type: ANIME, isAdult: false, search: "$escaped", sort: SEARCH_MATCH) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    suspend fun getRandom(): AniListMedia? {
        val list = getTrending().ifEmpty { getPopular() }
        if (list.isNotEmpty()) {
            return list.random()
        }
        val page = Random.nextInt(1, 10)
        val query = """
            query {
                Page(page: $page, perPage: 20) {
                    media(type: ANIME, isAdult: false, sort: POPULARITY_DESC) {
                        $mediaFragment
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).randomOrNull()
    }

    private suspend fun executeQuery(query: String): List<AniListMedia> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("query", query)
            }.toString()

            val request = Request.Builder()
                .url(graphQlUrl)
                .post(payload.toRequestBody(mediaType))
                .build()

            network.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.decodeFromString<ALPageResponse>(body)
                parsed.data?.Page?.media ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
// ANZ <--
