package com.example.xtreamtvapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.data.XtreamRepository
import com.example.xtreamtvapp.ui.player.PlayerActivity
import com.example.xtreamtvapp.ui.setup.SetupChoiceActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var editUrl: EditText
    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var textError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editUrl = findViewById(R.id.editXtreamUrl)
        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        btnLogin = findViewById(R.id.btnLogin)
        textError = findViewById(R.id.textError)

        btnLogin.setOnClickListener { doLogin() }

        // TV: request focus on first field for D-pad
        editUrl.requestFocus()
    }

    private fun doLogin() {
        val url = editUrl.text.toString().trim()
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString().trim()

        textError.visibility = View.GONE

        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            textError.text = getString(R.string.login_error)
            textError.visibility = View.VISIBLE
            return
        }

        btnLogin.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                XtreamRepository.login(url, username, password)
            }
            btnLogin.isEnabled = true
            result.fold(
                onSuccess = {
                    val normalizedUrl = XtreamRepository.normalizeBaseUrl(url)
                    val intent = Intent(this@LoginActivity, SetupChoiceActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_BASE_URL, normalizedUrl)
                        putExtra(PlayerActivity.EXTRA_USERNAME, username)
                        putExtra(PlayerActivity.EXTRA_PASSWORD, password)
                    }
                    startActivity(intent)
                    finish()
                },
                onFailure = { e ->
                    textError.text = e.message ?: getString(R.string.login_error)
                    textError.visibility = View.VISIBLE
                    Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
