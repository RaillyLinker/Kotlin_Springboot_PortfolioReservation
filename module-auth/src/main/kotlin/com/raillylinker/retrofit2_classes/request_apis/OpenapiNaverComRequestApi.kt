package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface OpenapiNaverComRequestApi {
    // [Naver Oauth2 AccessToken 요청]
    @GET("/v1/nid/me")
    fun getV1NidMe(
        @Header("Authorization") authorization: String
    ): Call<GetV1NidMeOutputVO?>

    // (naver profile 정보 요청 API 응답 객체)
    // https://openapi.naver.com/v1/nid/me
    data class GetV1NidMeOutputVO(
        @SerializedName("resultcode")
        @Expose
        val resultCode: String, // API 호출 결과 코드
        @SerializedName("message")
        @Expose
        val message: String, // 호출 결과 메시지
        @SerializedName("response")
        @Expose
        val response: ResponseVo
    ) {
        data class ResponseVo(
            @SerializedName("id")
            @Expose
            val id: String, // 동일인 식별 정보 네이버 아이디마다 고유하게 발급되는 유니크한 일련번호 값(API 호출 결과로 네이버 아이디값은 제공하지 않으며, 대신 'id' 라는 '애플리케이션당' 유니크한 일련번호값을 이용해서 자체적으로 회원정보를 구성하셔야 합니다.)
            @SerializedName("nickname")
            @Expose
            val nickname: String?, // 사용자 별명 (별명이 설정되어 있지 않으면 id*** 형태로 리턴됩니다.)
            @SerializedName("name")
            @Expose
            val name: String?, // 사용자 이름
            @SerializedName("email")
            @Expose
            val email: String?, // 사용자 메일 주소 기본적으로 네이버 내정보에 등록되어 있는 '기본 이메일' 즉 네이버ID@naver.com 값이나, 사용자가 다른 외부메일로 변경했을 경우는 변경된 이메일 주소로 됩니다.
            @SerializedName("gender")
            @Expose
            val gender: String?, // 성별 F: 여성 M: 남성 U: 확인불가
            @SerializedName("age")
            @Expose
            val age: String?, // 사용자 연령대
            @SerializedName("birthday")
            @Expose
            val birthday: String?, // 사용자 생일(MM-DD 형식)
            @SerializedName("profile_image")
            @Expose
            val profileImage: String?, // 사용자 프로필 사진 URL
            @SerializedName("birthyear")
            @Expose
            val birthyear: String?, // 출생연도
            @SerializedName("mobile")
            @Expose
            val mobile: String?, // 휴대전화번호
        )
    }
}