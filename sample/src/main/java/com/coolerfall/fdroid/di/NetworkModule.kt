package com.coolerfall.fdroid.di

import com.coolerfall.fdroid.BuildConfig
import com.coolerfall.fdroid.app.Constant
import com.coolerfall.fdroid.data.network.datasource.MarketDataSource
import com.coolerfall.fdroid.data.repository.MarketRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton

/**
 * Dagger module that provides network related collaborators.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

	@Provides @Singleton fun provideOkHttpClient(): OkHttpClient {
		val builder = OkHttpClient.Builder()
			.connectTimeout(30, SECONDS)
			.readTimeout(30, SECONDS)

		if (BuildConfig.DEBUG) {
			val loggingInterceptor = HttpLoggingInterceptor()
			loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
//			builder.addInterceptor(loggingInterceptor)
		}

		return builder.build()
	}

	@Provides @Singleton fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
		return Retrofit.Builder()
			.baseUrl(Constant.BASE_URL)
			.client(client)
			.addConverterFactory(MoshiConverterFactory.create(moshi))
			.build()
	}

	@Provides @Singleton fun provideMoshi(): Moshi {
		return Moshi.Builder()
			.addLast(KotlinJsonAdapterFactory())
			.build()
	}

	@Provides @Singleton
	fun provideDriveRepository(dataSource: MarketDataSource): MarketRepository {
		return dataSource
	}
}
