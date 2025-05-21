package com.example.greencodechallenge.di

import android.content.Context
import com.example.greencodechallenge.R
import com.example.greencodechallenge.data.api.ExchangeRateApi
import com.example.greencodechallenge.data.repository.ExchangeRateRepositoryImpl
import com.example.greencodechallenge.domain.repository.ExchangeRateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    private const val CACHE_TIMEOUT = 5 * 60 // 5 minutos en segundos

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache {
        return Cache(context.cacheDir, CACHE_SIZE.toLong())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        cache: Cache,
        @ApplicationContext context: Context
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url

                val newUrl = originalUrl.newBuilder()
                    .addQueryParameter("access_key", context.getString(R.string.API_KEY))
                    .build()

                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .build()

                chain.proceed(newRequest)
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val fromCache = response.cacheResponse != null
                val fromNetwork = response.networkResponse != null
                val cacheTimestamp = response.header("X-Cache-Timestamp")
                android.util.Log.d("CACHE", "From cache: $fromCache, From network: $fromNetwork, X-Cache-Timestamp: $cacheTimestamp")
                // Solo cachear respuestas exitosas
                if (response.isSuccessful) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=$CACHE_TIMEOUT")
                        .removeHeader("Pragma")
                        .removeHeader("Expires")
                        .header("X-Cache-Timestamp", System.currentTimeMillis().toString())
                        .build()
                } else {
                    response
                }
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(context.getString(R.string.BASE_URL))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi {
        return retrofit.create(ExchangeRateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExchangeRateRepository(api: ExchangeRateApi): ExchangeRateRepository {
        return ExchangeRateRepositoryImpl(api)
    }
} 