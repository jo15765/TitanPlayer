package com.example.xtreamtvapp.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.ui.country.CountrySelectionActivity
import com.example.xtreamtvapp.ui.player.PlayerActivity

/**
 * Shown after successful login. User picks Basic (all channels) or Advanced (country + preferences).
 */
class SetupChoiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_choice)

        val baseUrl = intent.getStringExtra(PlayerActivity.EXTRA_BASE_URL) ?: ""
        val username = intent.getStringExtra(PlayerActivity.EXTRA_USERNAME) ?: ""
        val password = intent.getStringExtra(PlayerActivity.EXTRA_PASSWORD) ?: ""

        if (baseUrl.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<Button>(R.id.btnBasicSetup).setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_BASE_URL, baseUrl)
                putExtra(PlayerActivity.EXTRA_USERNAME, username)
                putExtra(PlayerActivity.EXTRA_PASSWORD, password)
            }
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnAdvancedSetup).setOnClickListener {
            val intent = Intent(this, CountrySelectionActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_BASE_URL, baseUrl)
                putExtra(PlayerActivity.EXTRA_USERNAME, username)
                putExtra(PlayerActivity.EXTRA_PASSWORD, password)
            }
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnBasicSetup).requestFocus()
    }
}
