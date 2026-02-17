package com.example.xtreamtvapp.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Ensures category_id is always a string (API may return number or string).
 * Without this, Gson can leave String field empty when JSON has a number.
 */
class LiveCategoryDeserializer : JsonDeserializer<LiveCategory> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LiveCategory {
        val obj = json.asJsonObject
        val categoryId = when (val idEl = obj.get("category_id")) {
            null -> ""
            else -> when {
                idEl.isJsonPrimitive && idEl.asJsonPrimitive.isNumber -> idEl.asLong.toString()
                idEl.isJsonPrimitive && idEl.asJsonPrimitive.isString -> idEl.asString
                else -> idEl.toString().trim('"')
            }
        }
        val categoryName = obj.get("category_name")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        return LiveCategory(categoryId = categoryId, categoryName = categoryName)
    }
}
