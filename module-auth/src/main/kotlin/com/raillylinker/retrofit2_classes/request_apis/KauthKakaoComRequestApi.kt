package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface KauthKakaoComRequestApi {
    // [KakaoTalk Oauth2 AccessToken 요청]
    @POST("/oauth/token")
    @FormUrlEncoded
    fun postOOauthToken(
        // 무조건 "authorization_code" 입력
        @Field("grant_type") grantType: String,
        // OAuth2 ClientId
        @Field("client_id") clientId: String,
        // OAuth2 clientSecret
        @Field("client_secret") clientSecret: String,
        // OAuth2 로그인할때 사용한 Redirect Uri
        @Field("redirect_uri") redirectUri: String,
        // OAuth2 로그인으로 발급받은 코드
        @Field("code") code: String
    ): Call<PostOOauthTokenOutputVO?>

    data class PostOOauthTokenOutputVO(
        @SerializedName("access_token")
        @Expose
        val accessToken: String?,
        @SerializedName("expires_in")
        @Expose
        val expiresIn: Long?,
        @SerializedName("scope")
        @Expose
        val scope: String?,
        @SerializedName("token_type")
        @Expose
        val tokenType: String?
    )
}