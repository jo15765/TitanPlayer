package com.example.xtreamtvapp.ui.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.text.TextWatcher
import android.text.Editable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.data.LiveCategory
import com.example.xtreamtvapp.data.LiveStream
import com.example.xtreamtvapp.data.XtreamRepository
import com.example.xtreamtvapp.data.EpgEntry
import com.example.xtreamtvapp.data.currentProgramAt
import com.example.xtreamtvapp.data.displayTitle
import com.example.xtreamtvapp.data.formatTimeRange
import com.example.xtreamtvapp.data.toMillisRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Calendar
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var panelChannels: View
    private lateinit var playerContainer: View
    private lateinit var btnToggleChannels: Button
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var progressChannels: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnFullscreen: ImageButton
    private lateinit var panelEpg: View
    private lateinit var epgPlaceholder: View
    private lateinit var epgGridContainer: View
    private lateinit var epgGridView: EpgGridView
    private lateinit var categoryAdapter: CategoryAdapter
    private val epgEntriesByStreamId = mutableMapOf<Int, List<EpgEntry>>()

    private var exoPlayer: ExoPlayer? = null
    private var isFullscreen: Boolean = false
    private var isPlayerPlaying: Boolean = false
    private var selectedCategoryId: String? = null
    private var visibleCategories: List<LiveCategory> = emptyList()
    private var baseUrl: String = ""
    private var username: String = ""
    private var password: String = ""
    private var channels: List<LiveStream> = emptyList()
    private var allCategories: List<LiveCategory> = emptyList()
    private var filteredCategories: List<LiveCategory> = emptyList()
    private val selectedCountryNames: Set<String>? by lazy {
        intent.getStringArrayListExtra(EXTRA_SELECTED_COUNTRIES)?.toSet()?.takeIf { it.isNotEmpty() }
    }
    private val visibleCategoryIds = mutableSetOf<String>()
    private var panelExpanded: Boolean = true
    private var epgLoadJob: Job? = null

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: ""
        username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
        password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""

        if (baseUrl.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        playerView = findViewById(R.id.playerView)
        playerContainer = findViewById(R.id.playerContainer)
        panelChannels = findViewById(R.id.panelChannels)
        btnToggleChannels = findViewById(R.id.btnToggleChannels)
        recyclerCategories = findViewById(R.id.recyclerCategories)
        progressChannels = findViewById(R.id.progressChannels)
        btnBack = findViewById(R.id.btnBack)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        panelEpg = findViewById(R.id.panelEpg)
        epgPlaceholder = findViewById(R.id.epgPlaceholder)
        epgGridContainer = findViewById(R.id.epgGridContainer)
        epgGridView = findViewById(R.id.epgGridView)

        categoryAdapter = CategoryAdapter(emptyList(), null) { category -> onCategorySelected(category) }
        recyclerCategories.layoutManager = LinearLayoutManager(this)
        recyclerCategories.adapter = categoryAdapter

        epgGridView.onChannelRowClick = { stream -> playChannel(stream) }

        btnToggleChannels.setOnClickListener { toggleChannelPanel() }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { showCategorySettingsDialog() }
        btnFullscreen.setOnClickListener { enterFullscreen() }
        btnBack.setOnClickListener { onBackArrowClicked() }

        initPlayer()
        showEpgPanel()
        loadChannels()
    }

    @UnstableApi
    private fun initPlayer() {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("ViniPlayTV/1.0 (Android TV)")
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { player ->
                playerView.player = player
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        onPlaybackError(error)
                    }
                })
            }
    }

    private fun onPlaybackError(error: PlaybackException) {
        val msg = error.message ?: error.cause?.message ?: "Playback error"
        Log.e("PlayerActivity", "Playback failed: $msg", error)
        runOnUiThread { Toast.makeText(this, "Playback error: $msg", Toast.LENGTH_LONG).show() }
    }

    private fun loadChannels() {
        progressChannels.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                XtreamRepository.getLiveStreamsWithCategories(baseUrl, username, password)
            }
            progressChannels.visibility = View.GONE
            result.fold(
                onSuccess = { (categories, list) ->
                    channels = list
                    allCategories = categories
                    filteredCategories = filterCategoriesByCountry(categories)
                    if (visibleCategoryIds.isEmpty()) {
                        visibleCategoryIds.addAll(filteredCategories.map { it.categoryId })
                        visibleCategoryIds.add("_other_")
                        visibleCategoryIds.add("")
                    }
                    visibleCategories = buildVisibleCategoriesList(filteredCategories)
                    categoryAdapter.setCategories(visibleCategories, selectedCategoryId)
                    epgEntriesByStreamId.clear()
                    if (visibleCategories.isNotEmpty() && selectedCategoryId == null) {
                        selectedCategoryId = visibleCategories.first().categoryId
                        categoryAdapter.setCategories(visibleCategories, selectedCategoryId)
                        loadEpgForCategory(selectedCategoryId!!)
                    }
                },
                onFailure = { e ->
                    Toast.makeText(
                        this@PlayerActivity,
                        e.message ?: getString(R.string.no_channels),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun showCategorySettingsDialog() {
        val synthetic = listOf(
            LiveCategory("_other_", "Other"),
            LiveCategory("", "Channels")
        )
        val sortedCategories = (filteredCategories + synthetic)
            .distinctBy { it.categoryId }
            .sortedBy { it.categoryName.lowercase() }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_category_settings, null)
        val container = view.findViewById<LinearLayout>(R.id.containerCategoryChecks)
        val searchEdit = view.findViewById<EditText>(R.id.categorySearch)
        container.removeAllViews()

        data class CategoryRow(val categoryId: String, val check: CheckBox, val categoryName: String, val rowView: View)
        val rows = mutableListOf<CategoryRow>()
        for (cat in sortedCategories) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_category_checkbox, container, false)
            val check = item.findViewById<CheckBox>(R.id.checkCategory)
            val label = item.findViewById<TextView>(R.id.categoryLabel)
            label.text = cat.categoryName
            check.isChecked = cat.categoryId in visibleCategoryIds
            rows.add(CategoryRow(cat.categoryId, check, cat.categoryName, item))
            container.addView(item)
        }

        fun filterCategories(query: String) {
            val q = query.trim().lowercase()
            rows.forEach { row ->
                row.rowView.visibility = if (q.isEmpty() || row.categoryName.lowercase().contains(q)) View.VISIBLE else View.GONE
            }
        }
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCategories(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.btnCategorySettingsDone).setOnClickListener {
            visibleCategoryIds.clear()
            visibleCategoryIds.addAll(rows.filter { it.check.isChecked }.map { it.categoryId })
            visibleCategories = buildVisibleCategoriesList(filteredCategories)
            if (selectedCategoryId != null && visibleCategoryIds.contains(selectedCategoryId)) {
                categoryAdapter.setCategories(visibleCategories, selectedCategoryId)
            } else {
                selectedCategoryId = visibleCategories.firstOrNull()?.categoryId
                categoryAdapter.setCategories(visibleCategories, selectedCategoryId)
                if (selectedCategoryId != null) loadEpgForCategory(selectedCategoryId!!)
                else hideEpgGrid()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun toggleChannelPanel() {
        panelExpanded = !panelExpanded
        val params = panelChannels.layoutParams
        params.width = if (panelExpanded) resources.getDimensionPixelSize(R.dimen.panel_width_expanded) else resources.getDimensionPixelSize(R.dimen.panel_width_collapsed)
        panelChannels.layoutParams = params
        recyclerCategories.visibility = if (panelExpanded) View.VISIBLE else View.GONE
        btnToggleChannels.text = if (panelExpanded) getString(R.string.collapse) else getString(R.string.expand)
    }

    private fun playChannel(channel: LiveStream) {
        val url = channel.playUrl(baseUrl, username, password)
        Log.d("PlayerActivity", "Playing: $url")
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer?.apply {
            stop()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
        isPlayerPlaying = true
        showPlayerPanel()
        updateOverlayVisibility()
    }

    private fun enterFullscreen() {
        isFullscreen = true
        setGuidePanelWidth(0)
        panelChannels.visibility = View.GONE
        updateOverlayVisibility()
    }

    private fun exitFullscreen() {
        isFullscreen = false
        panelChannels.visibility = View.VISIBLE
        val width = if (panelExpanded) resources.getDimensionPixelSize(R.dimen.panel_width_expanded)
        else resources.getDimensionPixelSize(R.dimen.panel_width_collapsed)
        setGuidePanelWidth(width)
        updateOverlayVisibility()
    }

    private fun setGuidePanelWidth(widthPx: Int) {
        val params = panelChannels.layoutParams
        params.width = widthPx
        panelChannels.layoutParams = params
    }

    private fun onBackArrowClicked() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        isPlayerPlaying = false
        showEpgPanel()
        updateOverlayVisibility()
    }

    private fun showEpgPanel() {
        panelEpg.visibility = View.VISIBLE
        playerView.visibility = View.GONE
        findViewById<View>(R.id.playerOverlay).visibility = View.GONE
        selectedCategoryId?.let { id ->
            val channelList = getChannelsInCategory(id, allCategories, channels, visibleCategoryIds)
            if (channelList.isNotEmpty()) buildAndShowEpgGrid(channelList)
        }
    }

    private fun showPlayerPanel() {
        panelEpg.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        findViewById<View>(R.id.playerOverlay).visibility = View.VISIBLE
    }

    private fun filterCategoriesByCountry(categories: List<LiveCategory>): List<LiveCategory> {
        val countries = selectedCountryNames ?: return categories
        if (countries.isEmpty()) return categories
        val otherLabel = getString(R.string.other)
        return categories.filter { cat ->
            (cat.countryPrefix() ?: otherLabel) in countries
        }
    }

    private fun buildVisibleCategoriesList(categories: List<LiveCategory>): List<LiveCategory> {
        val synthetic = listOf(
            LiveCategory("_other_", "Other"),
            LiveCategory("", "Channels")
        )
        return (categories + synthetic)
            .distinctBy { it.categoryId }
            .filter { it.categoryId in visibleCategoryIds }
            .sortedBy { it.categoryName.lowercase() }
    }

    private fun onCategorySelected(category: LiveCategory) {
        selectedCategoryId = category.categoryId
        categoryAdapter.setCategories(visibleCategories, selectedCategoryId)
        loadEpgForCategory(category.categoryId)
    }

    private fun loadEpgForCategory(categoryId: String) {
        val channelList = getChannelsInCategory(categoryId, allCategories, channels, visibleCategoryIds)
        if (channelList.isEmpty()) {
            showEpgGridWithEmptyChannels()
            return
        }
        epgPlaceholder.visibility = View.GONE
        epgGridContainer.visibility = View.VISIBLE
        epgLoadJob?.cancel()
        epgLoadJob = lifecycleScope.launch {
            val batchSize = 3
            for (i in channelList.indices step batchSize) {
                val batch = channelList.drop(i).take(batchSize)
                withContext(Dispatchers.IO) {
                    batch.forEach { ch ->
                        XtreamRepository.getShortEpg(baseUrl, username, password, ch.streamId).getOrNull()
                            ?.let { list -> epgEntriesByStreamId[ch.streamId] = list }
                    }
                }
                withContext(Dispatchers.Main) {
                    buildAndShowEpgGrid(channelList)
                }
                if (i + batchSize < channelList.size) delay(60)
            }
        }
    }

    private fun showEpgGridWithEmptyChannels() {
        epgPlaceholder.visibility = View.GONE
        epgGridContainer.visibility = View.VISIBLE
        epgGridView.channelRows = emptyList()
        epgGridView.invalidate()
    }

    private fun hideEpgGrid() {
        epgGridContainer.visibility = View.GONE
        epgPlaceholder.visibility = View.VISIBLE
    }

    private fun buildAndShowEpgGrid(channelList: List<LiveStream>) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val minute = cal.get(Calendar.MINUTE)
        cal.set(Calendar.MINUTE, if (minute < 30) 0 else 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val timeWindowStart = cal.timeInMillis
        val timeWindowEnd = timeWindowStart + 6 * 60 * 60 * 1000L
        epgGridView.timeWindowStartMillis = timeWindowStart
        epgGridView.timeWindowEndMillis = timeWindowEnd
        epgGridView.nowMillis = now
        val gapLabel = getString(R.string.no_program_info)
        val rows = channelList.map { ch ->
            val entries = epgEntriesByStreamId[ch.streamId] ?: emptyList()
            val blocks = entries.mapNotNull { entry ->
                entry.toMillisRange()?.let { (startMs, endMs) ->
                    if (endMs <= timeWindowStart || startMs >= timeWindowEnd) return@mapNotNull null
                    EpgProgramBlock(
                        startMillis = startMs,
                        endMillis = endMs,
                        title = entry.displayTitle(),
                        isCurrent = now in startMs..endMs
                    )
                }
            }
            val filled = fillEpgGaps(blocks, timeWindowStart, timeWindowEnd, gapLabel)
            ch to filled
        }
        val totalBlocks = rows.sumOf { it.second.size }
        Log.d("PlayerActivity", "EPG grid: ${rows.size} channels, $totalBlocks blocks in window")
        epgGridView.channelRows = rows
    }

    /** Inserts placeholder blocks for any gaps so the EPG has no blank slots. */
    private fun fillEpgGaps(
        blocks: List<EpgProgramBlock>,
        timeWindowStart: Long,
        timeWindowEnd: Long,
        gapLabel: String
    ): List<EpgProgramBlock> {
        if (blocks.isEmpty()) {
            return listOf(EpgProgramBlock(timeWindowStart, timeWindowEnd, gapLabel, false))
        }
        val sorted = blocks.sortedBy { it.startMillis }
        val result = mutableListOf<EpgProgramBlock>()
        var current = timeWindowStart
        for (block in sorted) {
            val start = block.startMillis.coerceIn(timeWindowStart, timeWindowEnd)
            val end = block.endMillis.coerceIn(timeWindowStart, timeWindowEnd)
            if (start > current) {
                result.add(EpgProgramBlock(current, start, gapLabel, false))
            }
            result.add(block)
            current = maxOf(current, end)
        }
        if (current < timeWindowEnd) {
            result.add(EpgProgramBlock(current, timeWindowEnd, gapLabel, false))
        }
        return result.sortedBy { it.startMillis }
    }

    private fun updateOverlayVisibility() {
        if (isFullscreen) {
            btnBack.visibility = View.VISIBLE
            btnFullscreen.visibility = View.GONE
        } else if (isPlayerPlaying) {
            btnBack.visibility = View.VISIBLE
            btnFullscreen.visibility = View.VISIBLE
        } else {
            btnBack.visibility = View.GONE
            btnFullscreen.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFullscreen) {
            exitFullscreen()
            return
        }
        if (!panelExpanded) {
            toggleChannelPanel()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onDestroy() {
        epgLoadJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SELECTED_COUNTRIES = "selected_countries"
        const val EXTRA_PREF_LOCAL = "pref_local_channels"
        const val EXTRA_PREF_SPORTS = "pref_sports"
        const val EXTRA_PREF_MOVIES = "pref_movies"
        const val EXTRA_PREF_MLB_TEAM = "pref_mlb_team"
        const val EXTRA_PREF_NFL_TEAM = "pref_nfl_team"
        const val EXTRA_PREF_KIDS = "pref_kids"
    }
}
