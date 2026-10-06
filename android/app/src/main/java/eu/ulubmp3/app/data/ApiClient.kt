package eu.ulubmp3.app.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object ApiConfig {
    const val BASE_URL = "https://ulubmp3.eu/api/v1"
}

class UlubApiClient {
    suspend fun home(page: Int = 1): HomePayload = withContext(Dispatchers.IO) {
        val root = getJson("${ApiConfig.BASE_URL}/home?page=$page")
        HomePayload(
            top = parseTracks(root.optJSONArray("top") ?: JSONArray()),
            latest = parseTracks(root.optJSONArray("latest") ?: JSONArray()),
            categories = parseCategories(root.optJSONArray("categories") ?: JSONArray())
        )
    }

    suspend fun search(query: String, page: Int = 1): List<Track> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, Charsets.UTF_8.name())
        val root = getJson("${ApiConfig.BASE_URL}/search?q=$q&page=$page")
        parseTracks(root.optJSONArray("items") ?: JSONArray())
    }

    suspend fun category(categorySlug: String, page: Int = 1): List<Track> = withContext(Dispatchers.IO) {
        val slug = URLEncoder.encode(categorySlug, Charsets.UTF_8.name()).replace("+", "%20")
        val root = getJson("${ApiConfig.BASE_URL}/category/$slug?page=$page")
        parseTracks(root.optJSONArray("items") ?: JSONArray())
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "UlubMp3-Android/0.3")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code: $body")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCategories(array: JSONArray): List<Category> = buildList {
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            add(Category(item.getInt("id"), htmlDecode(item.optString("name")), item.optString("slug")))
        }
    }

    private fun parseTracks(array: JSONArray): List<Track> = buildList {
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            add(
                Track(
                    id = item.getInt("id"),
                    title = htmlDecode(item.optString("title")),
                    category = htmlDecode(item.optString("category")),
                    date = item.optString("date"),
                    imageUrl = nullableString(item, "image_url"),
                    audioUrl = nullableString(item, "audio_url"),
                    downloadUrl = nullableString(item, "download_url"),
                    size = nullableString(item, "size"),
                    bitrate = nullableString(item, "bitrate"),
                    downloads = item.optInt("downloads", 0),
                    pageUrl = nullableString(item, "url")
                )
            )
        }
    }

    private fun nullableString(item: JSONObject, key: String): String? {
        if (!item.has(key) || item.isNull(key)) return null
        return item.optString(key).trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun htmlDecode(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
}
