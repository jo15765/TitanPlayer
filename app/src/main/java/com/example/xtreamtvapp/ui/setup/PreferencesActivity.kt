package com.example.xtreamtvapp.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.ui.player.PlayerActivity

/**
 * Advanced set-up Page 2: preferences (local, sports, movies, MLB/NFL teams, kids).
 * Passes selections to PlayerActivity for future favorites / filtering.
 */
class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        val baseUrl = intent.getStringExtra(PlayerActivity.EXTRA_BASE_URL) ?: ""
        val username = intent.getStringExtra(PlayerActivity.EXTRA_USERNAME) ?: ""
        val password = intent.getStringExtra(PlayerActivity.EXTRA_PASSWORD) ?: ""
        val selectedCountries = intent.getStringArrayListExtra(PlayerActivity.EXTRA_SELECTED_COUNTRIES) ?: arrayListOf()

        if (baseUrl.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.pageIndicator).text = getString(R.string.page_num, 2)

        findViewById<Button>(R.id.btnPreferencesContinue).setOnClickListener {
            val prefs = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_BASE_URL, baseUrl)
                putExtra(PlayerActivity.EXTRA_USERNAME, username)
                putExtra(PlayerActivity.EXTRA_PASSWORD, password)
                putStringArrayListExtra(PlayerActivity.EXTRA_SELECTED_COUNTRIES, ArrayList(selectedCountries))
                putExtra(PlayerActivity.EXTRA_PREF_LOCAL, findViewById<CheckBox>(R.id.checkLocalChannels).isChecked)
                putExtra(PlayerActivity.EXTRA_PREF_SPORTS, findViewById<CheckBox>(R.id.checkSports).isChecked)
                putExtra(PlayerActivity.EXTRA_PREF_MOVIES, findViewById<CheckBox>(R.id.checkMovies).isChecked)
                putExtra(PlayerActivity.EXTRA_PREF_MLB_TEAM, findViewById<EditText>(R.id.editMlbTeam).text.toString().trim())
                putExtra(PlayerActivity.EXTRA_PREF_NFL_TEAM, findViewById<EditText>(R.id.editNflTeam).text.toString().trim())
                putExtra(PlayerActivity.EXTRA_PREF_KIDS, findViewById<CheckBox>(R.id.checkKids).isChecked)
            }
            startActivity(prefs)
            finish()
        }

        findViewById<CheckBox>(R.id.checkLocalChannels).requestFocus()
    }
}
