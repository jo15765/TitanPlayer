package com.example.xtreamtvapp.ui.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.xtreamtvapp.data.LiveStream

/** One program block for drawing: start/end in millis, title, and whether it's "current" (on air now). */
data class EpgProgramBlock(
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val isCurrent: Boolean
)

/**
 * Custom view that draws an EPG grid: time axis at top, channel names in a left column,
 * program blocks in a timeline with a current-time vertical line.
 */
class EpgGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var channelColumnWidthPx: Int = (180 * context.resources.displayMetrics.density).toInt()
    var rowHeightPx: Int = (48 * context.resources.displayMetrics.density).toInt()
    var slotWidthPx: Int = (100 * context.resources.displayMetrics.density).toInt()
    var timeWindowStartMillis: Long = 0L
    var timeWindowEndMillis: Long = 0L
    var nowMillis: Long = 0L

    /** (channel, list of program blocks in time window). Blocks should not overlap. */
    var channelRows: List<Pair<LiveStream, List<EpgProgramBlock>>> = emptyList()
        set(value) {
            field = value
            invalidate()
            requestLayout()
        }

    var onChannelRowClick: ((LiveStream) -> Unit)? = null

    private val timeWindowDurationMillis: Long
        get() = (timeWindowEndMillis - timeWindowStartMillis).coerceAtLeast(1L)

    private val timeHeaderHeightPx: Int = (32 * context.resources.displayMetrics.density).toInt()
    private val timelineWidthPx: Int
        get() = ((timeWindowEndMillis - timeWindowStartMillis) / (30 * 60 * 1000)).toInt().coerceAtLeast(1) * slotWidthPx

    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 14f * context.resources.displayMetrics.density
    }
    private val paintLabelMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99FFFFFF.toInt()
        textSize = 12f * context.resources.displayMetrics.density
    }
    private val paintCurrentBlock = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF26A69A.toInt() // teal
    }
    private val paintFutureBlock = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF424242.toInt()
    }
    private val paintGridLine = Paint().apply {
        color = 0x33FFFFFF
        strokeWidth = 1f
    }
    private val paintCurrentTimeLine = Paint().apply {
        color = 0xFF26A69A.toInt()
        strokeWidth = 3f
    }
    private val rect = RectF()
    private var lastTouchedRowIndex = -1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val contentWidth = channelColumnWidthPx + timelineWidthPx
        val contentHeight = timeHeaderHeightPx + channelRows.size * rowHeightPx
        setMeasuredDimension(
            resolveSize(contentWidth, widthMeasureSpec),
            resolveSize(contentHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val timelineWidth = timelineWidthPx
        val totalWidth = channelColumnWidthPx + timelineWidth

        // Time labels at top (in timeline area)
        val slotDurationMillis = 30 * 60 * 1000L
        var slotStart = (timeWindowStartMillis / slotDurationMillis) * slotDurationMillis
        var x = channelColumnWidthPx.toFloat()
        while (slotStart < timeWindowEndMillis && x < totalWidth) {
            val timeStr = formatTime(slotStart)
            canvas.drawText(timeStr, x + 4f, timeHeaderHeightPx - 8f, paintLabelMuted)
            canvas.drawLine(x, timeHeaderHeightPx.toFloat(), x, height.toFloat(), paintGridLine)
            x += slotWidthPx
            slotStart += slotDurationMillis
        }

        // Current time vertical line
        if (nowMillis in timeWindowStartMillis..timeWindowEndMillis) {
            val nx = channelColumnWidthPx + (nowMillis - timeWindowStartMillis).toFloat() / timeWindowDurationMillis * timelineWidth
            if (nx >= (channelColumnWidthPx + 1) && nx <= totalWidth - 1) {
                canvas.drawLine(nx, timeHeaderHeightPx.toFloat(), nx, height.toFloat(), paintCurrentTimeLine)
            }
        }

        channelRows.forEachIndexed { index, (channel, blocks) ->
            val rowTop = timeHeaderHeightPx + index * rowHeightPx
            val rowBottom = rowTop + rowHeightPx

            // Channel name (left column)
            val name = channel.name
            canvas.save()
            canvas.clipRect(0f, rowTop.toFloat(), channelColumnWidthPx.toFloat(), rowBottom.toFloat())
            canvas.drawText(name, 8f, rowTop + rowHeightPx / 2f + paintLabel.textSize / 3f, paintLabel)
            canvas.restore()

            // Program blocks (timeline area)
            val timelineLeft = channelColumnWidthPx.toFloat()
            blocks.forEach { block ->
                val left = timelineLeft + (block.startMillis - timeWindowStartMillis).toFloat() / timeWindowDurationMillis * timelineWidth
                val right = timelineLeft + (block.endMillis - timeWindowStartMillis).toFloat() / timeWindowDurationMillis * timelineWidth
                if (right <= timelineLeft || left >= timelineLeft + timelineWidth) return@forEach
                rect.set(
                    left.coerceIn(timelineLeft, timelineLeft + timelineWidth - 2),
                    rowTop + 4f,
                    right.coerceIn(timelineLeft + 2, timelineLeft + timelineWidth),
                    rowBottom - 4f
                )
                canvas.drawRoundRect(rect, 6f, 6f, if (block.isCurrent) paintCurrentBlock else paintFutureBlock)
                val title = block.title.take(30) + if (block.title.length > 30) "…" else ""
                canvas.save()
                canvas.clipRect(rect)
                canvas.drawText(title, rect.left + 6f, rect.centerY() + paintLabel.textSize / 3f, paintLabel)
                canvas.restore()
            }

            // Row separator
            canvas.drawLine(0f, rowBottom.toFloat(), totalWidth.toFloat(), rowBottom.toFloat(), paintGridLine)
        }
    }

    private fun formatTime(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("h:mma", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                val rowIndex = ((event.y - timeHeaderHeightPx) / rowHeightPx).toInt()
                if (rowIndex in channelRows.indices && event.x < channelColumnWidthPx) {
                    if (event.action == MotionEvent.ACTION_UP && rowIndex == lastTouchedRowIndex) {
                        onChannelRowClick?.invoke(channelRows[rowIndex].first)
                    }
                    lastTouchedRowIndex = rowIndex
                    return true
                }
                lastTouchedRowIndex = -1
            }
        }
        return super.onTouchEvent(event)
    }
}
