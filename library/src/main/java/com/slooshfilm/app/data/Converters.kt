package com.slooshfilm.app.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringToList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromListToString(list: List<String>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, String>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun fromStringToMapList(value: String?): Map<String, List<String>>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMapListToString(map: Map<String, List<String>>?): String? {
        return gson.toJson(map)
    }
}
