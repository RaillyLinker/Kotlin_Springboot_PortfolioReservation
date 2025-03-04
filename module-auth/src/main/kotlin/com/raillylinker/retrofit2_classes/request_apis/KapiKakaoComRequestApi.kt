package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

interface KapiKakaoComRequestApi {
    // [KakaoTalk Oauth2 AccessToken 요청]
    @GET("v2/user/me")
    @Headers("Connection: close")
    fun getV2UserMe(
        @Header("Authorization") authorization: String
    ): Call<GetV2UserMeOutputVO>

    // (kakao profile 정보 요청 API 응답 객체)
    // https://kapi.kakao.com/v2/user/me
    data class GetV2UserMeOutputVO(
        @SerializedName("id")
        @Expose
        val id: Long, // 회원번호
        @SerializedName("connected_at")
        @Expose
        val connectedAt: String?, // 서비스에 연결 완료된 시각, UTC* ("2022-11-19T09:29:50Z")
        @SerializedName("kakao_account")
        @Expose
        val kakaoAccount: KakaoAccountVo? // 카카오계정 정보
    ) {
        data class KakaoAccountVo(
            @SerializedName("profile_needs_agreement")
            @Expose
            val profileNeedsAgreement: Boolean?, // 사용자 동의 시 프로필 정보(닉네임/프로필 사진) 제공 가능
            @SerializedName("profile_nickname_needs_agreement")
            @Expose
            val profileNicknameNeedsAgreement: Boolean?, // 사용자 동의 시 닉네임 제공 가능
            @SerializedName("profile_image_needs_agreement")
            @Expose
            val profileImageNeedsAgreement: Boolean?, // 사용자 동의 시 프로필 사진 제공 가능
            @SerializedName("profile")
            @Expose
            val profile: ProfileVo?, // 프로필 정보
            @SerializedName("name_needs_agreement")
            @Expose
            val nameNeedsAgreement: Boolean?, // 사용자 동의 시 카카오계정 이름 제공 가능
            @SerializedName("name")
            @Expose
            val name: String?, // 카카오계정 이름
            @SerializedName("email_needs_agreement")
            @Expose
            val emailNeedsAgreement: Boolean?, // 사용자 동의 시 카카오계정 대표 이메일 제공 가능
            @SerializedName("is_email_valid")
            @Expose
            val isEmailValid: Boolean?, // 이메일 유효 여부 true: 유효한 이메일 false: 이메일이 다른 카카오계정에 사용돼 만료
            @SerializedName("is_email_verified")
            @Expose
            val isEmailVerified: Boolean?, // 이메일 인증 여부 true: 인증된 이메일 false: 인증되지 않은 이메일
            @SerializedName("email")
            @Expose
            val email: String?, // 카카오계정 대표 이메일
            @SerializedName("age_range_needs_agreement")
            @Expose
            val ageRangeNeedsAgreement: Boolean?, // 사용자 동의 시 연령대 제공 가능
            @SerializedName("age_range")
            @Expose
            val ageRange: String?, // 연령대 1~9: 1세 이상 10세 미만 10~14: 10세 이상 15세 미만 15~19: 15세 이상 20세 미만 20~29: 20세 이상 30세 미만 30~39: 30세 이상 40세 미만 40~49: 40세 이상 50세 미만 50~59: 50세 이상 60세 미만 60~69: 60세 이상 70세 미만 70~79: 70세 이상 80세 미만 80~89: 80세 이상 90세 미만 90~: 90세 이상
            @SerializedName("birthyear_needs_agreement")
            @Expose
            val birthyearNeedsAgreement: Boolean?, // 사용자 동의 시 출생 연도 제공 가능
            @SerializedName("birthyear")
            @Expose
            val birthyear: String?, // 출생 연도(YYYY 형식)
            @SerializedName("birthday_type")
            @Expose
            val birthdayType: String?, // 생일 타입 SOLAR(양력) 또는 LUNAR(음력)
            @SerializedName("gender_needs_agreement")
            @Expose
            val genderNeedsAgreement: Boolean?, // 사용자 동의 시 성별 제공 가능
            @SerializedName("gender")
            @Expose
            val gender: String?, // 성별 female: 여성 male: 남성
            @SerializedName("phone_number_needs_agreement")
            @Expose
            val phoneNumberNeedsAgreement: Boolean?, // 사용자 동의 시 전화번호 제공 가능
            @SerializedName("phone_number")
            @Expose
            val phoneNumber: String?, // 카카오계정의 전화번호 국내 번호인 경우 +82 00-0000-0000 형식해외 번호인 경우 자릿수, 붙임표(-) 유무나 위치가 다를 수 있음
            @SerializedName("ci_needs_agreement")
            @Expose
            val ciNeedsAgreement: Boolean?, // 사용자 동의 시 CI 참고 가능
            @SerializedName("ci")
            @Expose
            val ci: String?, // 연계정보
            @SerializedName("ci_authenticated_at")
            @Expose
            val ciAuthenticatedAt: String? // CI 발급 시각, UTC*
        ) {
            data class ProfileVo(
                @SerializedName("nickname")
                @Expose
                val nickname: String?, // 닉네임
                @SerializedName("thumbnail_image_url")
                @Expose
                val thumbnailImageUrl: String?, // 프로필 미리보기 이미지 URL 110px * 110px 또는 100px * 100px
                @SerializedName("profile_image_url")
                @Expose
                val profileImageUrl: String?, // 프로필 사진 URL 640px * 640px 또는 480px * 480px
                @SerializedName("is_default_image")
                @Expose
                val isDefaultImage: String? //프로필 사진 URL이 기본 프로필 사진 URL인지 여부 사용자가 등록한 프로필 사진이 없을 경우, 기본 프로필 사진 제공 true: 기본 프로필 사진 false: 사용자가 등록한 프로필 사진
            )
        }
    }
}