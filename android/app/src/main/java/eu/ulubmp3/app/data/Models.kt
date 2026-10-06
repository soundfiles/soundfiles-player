package eu.ulubmp3.app.data

data class Category(
    val id: Int,
    val name: String,
    val slug: String = ""
)

data class Track(
    val id: Int,
    val title: String,
    val category: String,
    val date: String,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val downloadUrl: String? = null,
    val size: String? = null,
    val bitrate: String? = null,
    val downloads: Int = 0,
    val pageUrl: String? = null
)

data class HomePayload(
    val top: List<Track>,
    val latest: List<Track>,
    val categories: List<Category>
)
