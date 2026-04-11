package com.example.workman

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object ProfanityFilter {
    private var profanityList: List<String> = emptyList()

    fun loadProfanityList(context: Context) {
        if (profanityList.isEmpty()) {
            val inputStream = context.resources.openRawResource(R.raw.profanity)
            val jsonText = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val jsonArray = JSONArray(jsonText)
            profanityList = (0 until jsonArray.length()).map { jsonArray.getString(it) }
        }
    }

    fun containsProfanity(message: String): Boolean {
        return profanityList.any { word ->
            message.contains(word, ignoreCase = true)
        }
    }

    fun cleanMessage(message: String): String {
        var result = message
        profanityList.forEach { word ->
            val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            result = result.replace(regex, "****")
        }
        return result
    }
}