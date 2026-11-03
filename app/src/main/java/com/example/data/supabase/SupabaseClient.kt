package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.SupabaseUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    // Safe retrieval of configuration injected via Secrets Gradle Plugin and .env/.env.example
    val baseUrl: String = try {
        val url = BuildConfig.SUPABASE_URL.trim()
        if (url.isNotBlank() && !url.contains("your-project-id") && !url.startsWith("YOUR_")) {
            if (url.endsWith("/")) url else "$url/"
        } else {
            "https://placeholder-project.supabase.co/"
        }
    } catch (e: Throwable) {
        "https://placeholder-project.supabase.co/"
    }

    val anonKey: String = try {
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (key.isNotBlank() && !key.contains("your-supabase-anon-key") && !key.startsWith("YOUR_")) {
            key
        } else {
            "anon-placeholder-token"
        }
    } catch (e: Throwable) {
        "anon-placeholder-token"
    }

    val isConfigured: Boolean
        get() = !baseUrl.contains("placeholder-project") && anonKey != "anon-placeholder-token"

    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    private var activeAccessToken: String? = null

    fun setAuthSession(user: SupabaseUser?) {
        _currentUser.value = user
        activeAccessToken = user?.accessToken
    }

    fun clearAuthSession() {
        _currentUser.value = null
        activeAccessToken = null
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = activeAccessToken ?: anonKey
        val request = original.newBuilder()
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }
}
