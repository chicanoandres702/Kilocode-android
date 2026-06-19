package com.kilocode.android.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sharedSecret: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Kilo-Auth", sharedSecret)
            .build()
        return chain.proceed(request)
    }
}
