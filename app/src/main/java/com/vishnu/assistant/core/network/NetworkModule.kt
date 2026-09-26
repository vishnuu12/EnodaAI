package com.vishnu.assistant.core.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds and holds the single Retrofit instance.
 *
 * BASE_URL points at the FastAPI backend running on the laptop
 * on the home Wi-Fi network. In Phase 15 this moves into build
 * configuration with a proper release setup (HTTPS).
 */
object NetworkModule {

    private const val BASE_URL = "http://192.168.1.7:8000/"

    val enodaApi: EnodaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EnodaApi::class.java)
    }
}
