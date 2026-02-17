package com.example.xtreamtvapp.ui.player

import com.example.xtreamtvapp.data.LiveCategory
import com.example.xtreamtvapp.data.LiveStream

/**
 * One row in the expandable channel list: either a category header or a channel.
 */
sealed class ChannelListRow {
    data class CategoryHeader(
        val category: LiveCategory,
        var expanded: Boolean = true
    ) : ChannelListRow()

    data class Channel(val stream: LiveStream, val categoryId: String) : ChannelListRow()
}

/**
 * Build flat list of rows from categories + streams. Uncategorized streams go under "Other".
 * @param visibleCategoryIds If non-null, only categories whose ID is in this set are shown in the guide.
 */
fun buildChannelListRows(
    categories: List<LiveCategory>,
    streams: List<LiveStream>,
    visibleCategoryIds: Set<String>? = null
): List<ChannelListRow> {
    fun streamCategoryId(stream: LiveStream): String =
        stream.categoryId?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: stream.categoryIds?.firstOrNull()?.toString() ?: ""

    val streamsByCategory = streams.groupBy { streamCategoryId(it) }
    val categoryIds = categories.map { it.categoryId }.toSet()
    val otherStreams: List<LiveStream> = (streamsByCategory[""] ?: emptyList<LiveStream>()) +
        streamsByCategory.filterKeys { it !in categoryIds }.values.flatten()

    fun includeCategory(id: String): Boolean =
        visibleCategoryIds == null || id in visibleCategoryIds

    val rows = mutableListOf<ChannelListRow>()
    for (cat in categories) {
        val streamsInCat = streamsByCategory[cat.categoryId] ?: emptyList()
        if (streamsInCat.isEmpty() || !includeCategory(cat.categoryId)) continue
        rows.add(ChannelListRow.CategoryHeader(category = cat, expanded = false))
        rows.addAll(streamsInCat.map { ChannelListRow.Channel(it, cat.categoryId) })
    }
    if (otherStreams.isNotEmpty() && includeCategory("_other_")) {
        val otherCat = LiveCategory(categoryId = "_other_", categoryName = "Other")
        rows.add(ChannelListRow.CategoryHeader(category = otherCat, expanded = false))
        rows.addAll(otherStreams.map { ChannelListRow.Channel(it, otherCat.categoryId) })
    }
    if (rows.isEmpty() && streams.isNotEmpty() && includeCategory("")) {
        val allCat = LiveCategory(categoryId = "", categoryName = "Channels")
        rows.add(ChannelListRow.CategoryHeader(category = allCat, expanded = false))
        rows.addAll(streams.map { ChannelListRow.Channel(it, "") })
    }
    return rows
}

/** Channels in one category, alphabetically by name. Uses same grouping as [buildChannelListRows]. */
fun getChannelsInCategory(
    categoryId: String,
    categories: List<LiveCategory>,
    streams: List<LiveStream>,
    visibleCategoryIds: Set<String>?
): List<LiveStream> {
    fun streamCategoryId(stream: LiveStream): String =
        stream.categoryId?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: stream.categoryIds?.firstOrNull()?.toString() ?: ""

    val streamsByCategory = streams.groupBy { streamCategoryId(it) }
    val categoryIds = categories.map { it.categoryId }.toSet()
    val otherStreams: List<LiveStream> = (streamsByCategory[""] ?: emptyList()) +
        streamsByCategory.filterKeys { it !in categoryIds }.values.flatten()

    val key = categoryId.toString().trim()
    val list = when (key) {
        "_other_" -> otherStreams
        "" -> streams
        else -> streamsByCategory[key] ?: emptyList()
    }
    return list.sortedBy { it.name.lowercase() }
}
