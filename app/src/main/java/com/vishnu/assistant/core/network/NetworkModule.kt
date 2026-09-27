package com.vishnu.assistant.core.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds and holds the single Retrofit instance.
 *
 * BASE_URL points to the public EnodaAI FastAPI backend.
 */
object NetworkModule {

    private const val BASE_URL = "https://enodaai.onrender.com/"
    //private const val BASE_URL = "http://192.168.1.7:8000/"

    val enodaApi: EnodaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EnodaApi::class.java)
    }
}