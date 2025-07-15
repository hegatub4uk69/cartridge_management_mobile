package com.mobile.cartridgemanagement.ui.network

import okhttp3.Interceptor
import okhttp3.Response
import java.net.CookieManager

class CsrfInterceptor(private val cookieManager: CookieManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Добавляем CSRF-токен только для POST/PUT/DELETE/PATCH
        if (request.method != "GET") {
            val csrfToken = cookieManager.cookieStore.cookies
                .firstOrNull { it.name == "csrftoken" }?.value
                ?: ""

            val newRequest = request.newBuilder()
                .header("X-CSRFToken", csrfToken)
                .build()

            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }
}