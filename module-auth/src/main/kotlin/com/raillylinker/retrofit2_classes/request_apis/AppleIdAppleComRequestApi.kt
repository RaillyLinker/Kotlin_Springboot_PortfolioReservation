package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

interface AppleIdAppleComRequestApi {
    // (Apple IdToken 해독을 위한 공개키 가져오기)
    @GET("auth/keys")
    @Headers("Content-Type: application/json")
    fun getAuthKeys(): Call<GetAuthKeysOutputVo>

    data class GetAuthKeysOutputVo(
        @SerializedName("keys")
        @Expose
        val keys: List<Key>,
    ) {
        data class Key(
            @SerializedName("kty")
            @Expose
            val kty: String,
            @SerializedName("kid")
            @Expose
            val kid: String,
            @SerializedName("use")
            @Expose
            val use: String,
            @SerializedName("alg")
            @Expose
            val alg: String,
            @SerializedName("n")
            @Expose
            val n: String,
            @SerializedName("e")
            @Expose
            val e: String
        )
    }
}