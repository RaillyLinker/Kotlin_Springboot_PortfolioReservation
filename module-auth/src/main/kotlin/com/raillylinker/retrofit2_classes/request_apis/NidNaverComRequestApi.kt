package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NidNaverComRequestApi {
    // [Naver Oauth2 AccessToken 요청]
    @GET("/oauth2.0/token")
    fun getOAuth2Dot0Token(
        // 무조건 "authorization_code" 입력
        @Query("grant_type") grantType: String,
        // OAuth2 ClientId
        @Query("client_id") clientId: String,
        // OAuth2 clientSecret
        @Query("client_secret") clientSecret: String,
        // OAuth2 로그인할때 사용한 Redirect Uri
        @Query("redirect_uri") redirectUri: String,
        // OAuth2 로그인으로 발급받은 코드
        @Query("code") code: String,
        // OAuth2 로그인할때 사용한 state
        @Query("state") state: String
    ): Call<GetOAuth2Dot0TokenRequestOutputVO?>

    data class GetOAuth2Dot0TokenRequestOutputVO(
        @SerializedName("access_token")
        @Expose
        val accessToken: String?,
        @SerializedName("refresh_token")
        @Expose
        val refreshToken: String?,
        @SerializedName("token_type")
        @Expose
        val tokenType: String?,
        @SerializedName("expires_in")
        @Expose
        val expiresIn: Long?
    )
}