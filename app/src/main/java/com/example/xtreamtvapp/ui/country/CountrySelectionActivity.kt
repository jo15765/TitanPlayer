package com.example.xtreamtvapp.ui.country

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.data.LiveCategory
import com.example.xtreamtvapp.data.XtreamRepository
import com.example.xtreamtvapp.ui.player.PlayerActivity
import com.example.xtreamtvapp.ui.setup.PreferencesActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown after successful login. Fetches categories, infers countries from names
 * (e.g. "USA | Local ABC" -> USA), lets user select which countries to include.
 * Passes selection to PlayerActivity to filter categories.
 */
class CountrySelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_selection)

        val baseUrl = intent.getStringExtra(PlayerActivity.EXTRA_BASE_URL) ?: ""
        val username = intent.getStringExtra(PlayerActivity.EXTRA_USERNAME) ?: ""
        val password = intent.getStringExtra(PlayerActivity.EXTRA_PASSWORD) ?: ""

        if (baseUrl.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val progress = findViewById<ProgressBar>(R.id.progressCountries)
        val recycler = findViewById<RecyclerView>(R.id.recyclerCountries)
        val btnNext = findViewById<Button>(R.id.btnCountryNext)
        findViewById<TextView>(R.id.pageIndicator).text = getString(R.string.page_num, 1)

        progress.visibility = View.VISIBLE
        recycler.layoutManager = GridLayoutManager(this, 4)
        recycler.setHasFixedSize(false)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                XtreamRepository.getLiveCategories(baseUrl, username, password)
            }
            progress.visibility = View.GONE
            result.fold(
                onSuccess = { categories ->
                    val countries = buildCountryList(categories)
                    if (countries.isEmpty()) {
                        Toast.makeText(this@CountrySelectionActivity, R.string.no_channels, Toast.LENGTH_SHORT).show()
                        openPreferences(baseUrl, username, password, emptyList())
                        return@fold
                    }
                    val adapter = CountryAdapter(countries)
                    recycler.adapter = adapter
                    btnNext.setOnClickListener {
                        openPreferences(baseUrl, username, password, adapter.getSelectedCountries())
                    }
                    btnNext.requestFocus()
                },
                onFailure = { e ->
                    Toast.makeText(this@CountrySelectionActivity, e.message ?: getString(R.string.login_error), Toast.LENGTH_LONG).show()
                    finish()
                }
            )
        }
    }

    private fun buildCountryList(categories: List<LiveCategory>): List<String> {
        val set = mutableSetOf<String>()
        categories.forEach { cat ->
            set.add(cat.countryPrefix() ?: getString(R.string.other))
        }
        return set.sorted()
    }

    private fun openPreferences(baseUrl: String, username: String, password: String, selectedCountries: List<String>) {
        val intent = Intent(this, PreferencesActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_BASE_URL, baseUrl)
            putExtra(PlayerActivity.EXTRA_USERNAME, username)
            putExtra(PlayerActivity.EXTRA_PASSWORD, password)
            putStringArrayListExtra(PlayerActivity.EXTRA_SELECTED_COUNTRIES, ArrayList(selectedCountries))
        }
        startActivity(intent)
        finish()
    }
}
