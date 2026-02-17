package com.example.xtreamtvapp.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.data.LiveStream

class ChannelAdapter(
    private var allRows: List<ChannelListRow>,
    private val onChannelClick: (LiveStream) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val collapsedCategoryIds = mutableSetOf<String>()
    private var selectedStreamId: Int = -1
    private val epgByStreamId = mutableMapOf<Int, String>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHANNEL = 1
    }

    /** Visible rows (headers + channels whose category is expanded) */
    private fun visibleRows(): List<ChannelListRow> {
        return allRows.filter { row ->
            when (row) {
                is ChannelListRow.CategoryHeader -> true
                is ChannelListRow.Channel -> row.categoryId !in collapsedCategoryIds
            }
        }
    }

    fun setRows(rows: List<ChannelListRow>) {
        allRows = rows
        // Start with all categories collapsed (user taps + to expand)
        collapsedCategoryIds.clear()
        collapsedCategoryIds.addAll(
            rows.filterIsInstance<ChannelListRow.CategoryHeader>().map { it.category.categoryId }
        )
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (visibleRows()[position]) {
            is ChannelListRow.CategoryHeader -> TYPE_HEADER
            is ChannelListRow.Channel -> TYPE_CHANNEL
        }
    }

    override fun getItemCount(): Int = visibleRows().size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view) { categoryId ->
                    toggleCategory(categoryId)
                }
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_channel, parent, false)
                ChannelViewHolder(view, onChannelClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = visibleRows()[position]) {
            is ChannelListRow.CategoryHeader -> (holder as CategoryHeaderViewHolder).bind(row)
            is ChannelListRow.Channel -> (holder as ChannelViewHolder).bind(
                row.stream,
                row.stream.streamId == selectedStreamId,
                epgByStreamId[row.stream.streamId]
            )
        }
    }

    /** Update EPG for one channel and refresh only that row. Call from main thread. */
    fun updateEpg(streamId: Int, nowTitle: String?) {
        if (epgByStreamId[streamId] == nowTitle) return
        epgByStreamId[streamId] = nowTitle ?: ""
        val visible = visibleRows()
        val pos = visible.indexOfFirst { (it as? ChannelListRow.Channel)?.stream?.streamId == streamId }
        if (pos >= 0) notifyItemChanged(pos)
    }

    private fun toggleCategory(categoryId: String) {
        val expanding = categoryId in collapsedCategoryIds
        val visibleBefore = visibleRows()
        val headerIndex = visibleBefore.indexOfFirst { (it as? ChannelListRow.CategoryHeader)?.category?.categoryId == categoryId }
        if (headerIndex < 0) return
        val categoryStartInAll = allRows.indexOfFirst { (it as? ChannelListRow.CategoryHeader)?.category?.categoryId == categoryId }
        val channelsInCategory = if (categoryStartInAll >= 0) {
            allRows.drop(categoryStartInAll + 1).takeWhile { it is ChannelListRow.Channel && it.categoryId == categoryId }.size
        } else 0
        if (expanding) {
            collapsedCategoryIds.remove(categoryId)
            allRows.filterIsInstance<ChannelListRow.CategoryHeader>().find { it.category.categoryId == categoryId }?.expanded = true
            notifyItemChanged(headerIndex)
            notifyItemRangeInserted(headerIndex + 1, channelsInCategory)
        } else {
            collapsedCategoryIds.add(categoryId)
            allRows.filterIsInstance<ChannelListRow.CategoryHeader>().find { it.category.categoryId == categoryId }?.expanded = false
            notifyItemRangeRemoved(headerIndex + 1, channelsInCategory)
            notifyItemChanged(headerIndex)
        }
    }

    fun updateSelectedStreamId(streamId: Int) {
        val old = selectedStreamId
        selectedStreamId = streamId
        val visible = visibleRows()
        val oldIdx = visible.indexOfFirst { (it as? ChannelListRow.Channel)?.stream?.streamId == old }
        val newIdx = visible.indexOfFirst { (it as? ChannelListRow.Channel)?.stream?.streamId == streamId }
        if (oldIdx >= 0) notifyItemChanged(oldIdx)
        if (newIdx >= 0) notifyItemChanged(newIdx)
    }

    class CategoryHeaderViewHolder(
        itemView: View,
        private val onToggle: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val expandIcon: TextView = itemView.findViewById(R.id.categoryExpandIcon)
        private val focusBg: View = itemView.findViewById(R.id.categoryFocusBg)

        fun bind(header: ChannelListRow.CategoryHeader) {
            categoryName.text = header.category.categoryName
            expandIcon.text = if (header.expanded) "−" else "+"
            focusBg.visibility = View.GONE
            itemView.setOnClickListener {
                header.expanded = !header.expanded
                onToggle(header.category.categoryId)
            }
            itemView.isFocusable = true
            itemView.setOnFocusChangeListener { _, hasFocus -> focusBg.visibility = if (hasFocus) View.VISIBLE else View.GONE }
        }
    }

    class ChannelViewHolder(
        itemView: View,
        private val onChannelClick: (LiveStream) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.channelName)
        private val epgText: TextView = itemView.findViewById(R.id.channelEpg)
        private val focusBg: View = itemView.findViewById(R.id.channelFocusBg)

        fun bind(channel: LiveStream, selected: Boolean, nowTitle: String?) {
            name.text = channel.name
            if (!nowTitle.isNullOrBlank()) {
                epgText.text = itemView.context.getString(R.string.now_playing, nowTitle)
                epgText.visibility = View.VISIBLE
            } else {
                epgText.visibility = View.GONE
            }
            focusBg.visibility = if (selected) View.VISIBLE else View.GONE
            itemView.setOnClickListener { onChannelClick(channel) }
            itemView.isFocusable = true
            itemView.setOnFocusChangeListener { _, hasFocus ->
                focusBg.visibility = if (hasFocus || selected) View.VISIBLE else View.GONE
            }
        }
    }
}
