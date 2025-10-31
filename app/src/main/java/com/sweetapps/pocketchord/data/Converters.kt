package com.sweetapps.pocketchord.data

import androidx.room.TypeConverter

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromIntListToCsv(list: List<Int>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    @JvmStatic
    fun toIntListFromCsv(csv: String?): List<Int>? {
        return csv?.split(",")?.map { it.trim().toInt() }
    }

    @TypeConverter
    @JvmStatic
    fun fromStringListToCsv(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    @JvmStatic
    fun toStringListFromCsv(csv: String?): List<String>? {
        return csv?.split(",")?.map { it.trim() }
    }
}

