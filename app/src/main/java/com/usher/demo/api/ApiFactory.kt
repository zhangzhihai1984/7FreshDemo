package com.usher.demo.api

import android.annotation.SuppressLint
import com.google.gson.GsonBuilder
import com.usher.demo.api.entities.CartEntity
import com.usher.demo.api.entities.DetailEntity
import com.usher.demo.api.entities.ResultEntity
import com.usher.demo.util.RxUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class ApiFactory private constructor() {
    companion object {
        private const val BASE_URL = "https://discovery.bclsmartlife.com/"

        val instance: ApiFactory by lazy { ApiFactory() }
    }

    private var mApiService: ApiService

    init {
        mApiService = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
                .create(ApiService::class.java)
    }

    private fun getClient(): OkHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(getSSLSocketFactory(), getTrustManager()[0] as X509TrustManager)
            .hostnameVerifier(getHostnameVerifier())
            .protocols(listOf(Protocol.HTTP_1_1))
//        .addInterceptor(getHeaderInterceptor())
            .build()

    private fun getSSLSocketFactory(): SSLSocketFactory = SSLContext.getInstance("SSL")
            .apply { init(null, getTrustManager(), SecureRandom()) }.socketFactory

    private fun getTrustManager(): Array<TrustManager> =
            arrayOf(object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

    private fun getHostnameVerifier(): HostnameVerifier = HostnameVerifier { _, _ -> true }

    private fun <T> getResultComposer(): ObservableTransformer<T, ResultEntity<T>> =
            ObservableTransformer { upstream ->
                upstream.map { resp -> ResultEntity(resp) }
                        .onErrorReturn { err ->
                            var result = ResultEntity<T>(null)
                            if (err is HttpException) {
                                err.response()?.errorBody()?.run {
                                    result = ResultEntity(null)
                                }
                            }

                            result
                        }
            }

    fun getDetail(): Observable<ResultEntity<DetailEntity>> =
            mApiService.getDetail()
                    .compose(RxUtil.getSchedulerComposer())
                    .compose(getResultComposer())

    fun getCart(): Observable<ResultEntity<CartEntity>> =
            mApiService.getCart()
                    .compose(RxUtil.getSchedulerComposer())
                    .compose(getResultComposer())
}