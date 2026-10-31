// ANZ -->
package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class AniListMedia(
    val id: Long,
    val title: ALMediaTitle? = null,
    val description: String? = null,
    val bannerImage: String? = null,
    val coverImage: ALMediaCoverImage? = null,
    val genres: List<String>? = emptyList(),
    val averageScore: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val seasonYear: Int? = null,
    val season: String? = null,
) {
    val displayTitle: String
        get() = title?.english?.takeIf { it.isNotBlank() }
            ?: title?.romaji?.takeIf { it.isNotBlank() }
            ?: title?.userPreferred?.takeIf { it.isNotBlank() }
            ?: "Unknown"

    val posterUrl: String?
        get() = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium
}

@Serializable
data class ALMediaTitle(
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null,
    val userPreferred: String? = null,
)

@Serializable
data class ALMediaCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
)

@Serializable
data class ALPageResponse(
    val data: ALPageData? = null,
)

@Serializable
data class ALPageData(
    val Page: ALPage? = null,
)

@Serializable
data class ALPage(
    val media: List<AniListMedia> = emptyList(),
)
// ANZ <--
