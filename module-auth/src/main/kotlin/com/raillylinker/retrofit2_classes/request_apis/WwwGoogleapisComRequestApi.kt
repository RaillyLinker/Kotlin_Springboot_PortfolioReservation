package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

// (한 주소에 대한 API 요청명세)
// 사용법은 아래 기본 사용 샘플을 참고하여 추상함수를 작성하여 사용
interface WwwGoogleapisComRequestApi {
    // [Google Oauth2 AccessToken 요청]
    @GET("/oauth2/v1/userinfo")
    fun getOauth2V1UserInfo(
        @Header("Authorization") authorization: String
    ): Call<GetOOauth2V1UserInfoOutputVO?>

    data class GetOOauth2V1UserInfoOutputVO(
        @SerializedName("id")
        @Expose
        val id: String?,
        @SerializedName("email")
        @Expose
        val email: String?,
        @SerializedName("verified_email")
        @Expose
        val verifiedEmail: Boolean?,
        @SerializedName("name")
        @Expose
        val name: String?,
        @SerializedName("given_name")
        @Expose
        val givenName: String?,
        @SerializedName("family_name")
        @Expose
        val familyName: String?,
        @SerializedName("picture")
        @Expose
        val picture: String?,
        @SerializedName("locale")
        @Expose
        val locale: String?
    )
}