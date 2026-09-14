package com.example.one.exercise

import android.content.Context
import org.json.JSONArray

/**
 * Reads the bundled exercise catalog (app/src/main/assets/exercises.json), trimmed from
 * https://github.com/hasaneyldrm/exercises-dataset. Media (images/gifs) stays hosted on that
 * repo's CDN mirror rather than being bundled, since it's copyrighted (see EXERCISE_MEDIA_ATTRIBUTION).
 */
object ExerciseCatalogLoader {

    private const val MEDIA_BASE_URL = "https://cdn.jsdelivr.net/gh/hasaneyldrm/exercises-dataset@main/"
    private const val ASSET_FILE_NAME = "exercises.json"

    fun load(context: Context): List<Exercise> {
        val json = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    Exercise(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        bodyPart = obj.getString("bodyPart"),
                        equipment = obj.getString("equipment"),
                        target = obj.getString("target"),
                        secondaryMuscles = obj.getJSONArray("secondaryMuscles").toStringList(),
                        instructions = obj.getJSONArray("instructions").toStringList(),
                        imageUrl = MEDIA_BASE_URL + obj.getString("image"),
                        gifUrl = MEDIA_BASE_URL + obj.getString("gifUrl")
                    )
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        buildList(length()) { for (i in 0 until this@toStringList.length()) add(getString(i)) }
}
