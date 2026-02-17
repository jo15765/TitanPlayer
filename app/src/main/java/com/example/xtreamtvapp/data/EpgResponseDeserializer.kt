package com.example.xtreamtvapp.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Accepts either { "epg_listings": [...] } or raw array for EPG API.
 */
class EpgResponseDeserializer : JsonDeserializer<EpgResponse> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EpgResponse {
        val listType = object : TypeToken<List<EpgEntry>>() {}.type
        return when {
            json.isJsonArray -> EpgResponse(epgListings = context.deserialize(json, listType))
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val arr = when {
                    obj.has("epg_listings") -> obj.getAsJsonArray("epg_listings")
                    obj.has("epg_listing") -> obj.getAsJsonArray("epg_listing")
                    obj.has("data") -> obj.getAsJsonArray("data")
                    obj.has("events") -> obj.getAsJsonArray("events")
                    else -> null
                }
                if (arr != null) EpgResponse(epgListings = context.deserialize(arr, listType))
                else EpgResponse(epgListings = emptyList())
            }
            else -> EpgResponse(epgListings = emptyList())
        }
    }
}
