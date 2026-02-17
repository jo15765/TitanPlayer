package com.example.xtreamtvapp.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xtreamtvapp.R

/**
 * Row for the EPG panel: channel name, current program title, time range.
 */
data class EpgPanelRow(
    val channelName: String,
    val programTitle: String,
    val timeRange: String
)

class EpgPanelAdapter(
    private var rows: List<EpgPanelRow>
) : RecyclerView.Adapter<EpgPanelAdapter.ViewHolder>() {

    fun setRows(newRows: List<EpgPanelRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_epg_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount(): Int = rows.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val channelName: TextView = itemView.findViewById(R.id.epgRowChannelName)
        private val programTitle: TextView = itemView.findViewById(R.id.epgRowProgramTitle)
        private val timeRange: TextView = itemView.findViewById(R.id.epgRowTime)

        fun bind(row: EpgPanelRow) {
            channelName.text = row.channelName
            programTitle.text = row.programTitle
            timeRange.text = row.timeRange
            timeRange.visibility = if (row.timeRange.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
