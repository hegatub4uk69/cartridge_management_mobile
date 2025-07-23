package com.mobile.cartridgemanagement

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mobile.cartridgemanagement.ui.network.ApiClient
import com.mobile.cartridgemanagement.ui.network.ApiService
import com.mobile.cartridgemanagement.ui.network.requests.LoginRequest
import androidx.core.content.edit

class AuthPage : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    private val apiService = ApiClient.retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUserLoggedIn()) {
            redirectToMain()
            finish()
            return
        }
        setContentView(R.layout.auth_page)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        // Инициализация элементов через findViewById
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val request = LoginRequest(
                username = etUsername.text.toString(),
                password = etPassword.text.toString()
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = apiService.login(request)
                    runOnUiThread {
                        Toast.makeText(
                            this@AuthPage,
                            response.result,
                            Toast.LENGTH_SHORT
                        ).show()

                        if (response.result == "Авторизация прошла успешно!") {

                            // Упрощенный и безопасный переход
                            Intent(this@AuthPage, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(this)
                            }
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AuthPage,
                            "Ошибка: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    private fun modifyUserSharedPrefs(departmentId: Int?) {
        var context = this.applicationContext
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        prefs.edit { putString("department_id", departmentId.toString()) }
    }

    private fun isUserLoggedIn(): Boolean {
        // Проверка наличия куки сессии
        val cookieManager = CookieManager.getInstance()
        return cookieManager.getCookie("sessionid")?.isNotEmpty() ?: false
    }

    private fun redirectToMain() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}