package ru.doshikologiya.nativepreview

import org.json.JSONArray
import org.json.JSONObject

internal data class RecipeIndexEntry(val id: String, val path: String, val hash: String)
internal data class RemoteRecipePreview(
    val added: Int, val updated: Int,
    val entries: List<RecipeIndexEntry> = emptyList(),
    val changed: List<Recipe> = emptyList()
)

/** The same JSON.stringify + UTF-16 FNV-1a contract as js/core/api.js. */
internal object RecipeUpdates {
    fun stringify(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().joinToString(",", "{", "}") {
            stringify(it) + ":" + stringify(value.get(it))
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") {
            stringify(value.get(it))
        }
        is String -> buildString {
            append('"')
            for (char in value) when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
            }
            append('"')
        }
        is Number -> JSONObject.numberToString(value)
        else -> value.toString()
    }

    fun fingerprint(json: String): String {
        var hash = 2166136261L.toInt()
        stringify(JSONObject(json)).forEach { hash = (hash xor it.code) * 16777619 }
        return Integer.toUnsignedString(hash, 16).padStart(8, '0')
    }

    fun diff(current: List<Recipe>, json: String): RemoteRecipePreview {
        val index = JSONObject(json)
        require(index.getInt("schemaVersion") == 1)
        val entries = index.getJSONArray("recipes")
        require(entries.length() > 0)
        val seen = mutableSetOf<String>()
        val before = current.associateBy { it.id }
        val changes = (0 until entries.length()).map { i ->
            val entry = entries.getJSONObject(i)
            val id = entry.getString("id")
            val path = entry.getString("path")
            val hash = entry.getString("hash")
            require(id.matches(Regex("^[a-z0-9-]+$")) && seen.add(id) &&
                path == "recipe-entries/$id.json" && hash.matches(Regex("^[a-f0-9]{8}$")))
            RecipeIndexEntry(id, path, hash)
        }.filter { entry -> before[entry.id]?.let { fingerprint(it.sourceJson) } != entry.hash }
        return RemoteRecipePreview(changes.count { it.id !in before },
            changes.count { it.id in before }, entries = changes)
    }

    fun verify(entry: RecipeIndexEntry, json: String): Recipe {
        val recipe = RecipeCatalog.parse("[$json]").single()
        require(recipe.id == entry.id && fingerprint(json) == entry.hash) {
            "Проверка целостности не пройдена: ${entry.id}"
        }
        return recipe
    }

    fun merge(current: List<Recipe>, changed: List<Recipe>): List<Recipe> =
        LinkedHashMap(current.associateBy { it.id }).apply {
            changed.forEach { put(it.id, it) }
        }.values.toList()
}
