package com.raillylinker.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import com.raillylinker.services.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

// API 의 로그인/비로그인 시의 결과를 달리하고 싶으면, 차라리 API 를 2개로 나누는 것이 더 좋습니다.
// SpringSecurity 의 도움을 받지 않는다면 복잡한 인증 관련 코드를 적용하여 로그인 여부를 확인해야 하므로, API 코드가 지저분해지기 때문이죠.
@Tag(name = "/auth APIs", description = "token 인증/인가 API 컨트롤러")
@Controller
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    @Operation(
        summary = "비 로그인 접속 테스트",
        description = "비 로그인 접속 테스트용 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @GetMapping(
        path = ["/for-no-logged-in"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    @ResponseBody
    fun noLoggedInAccessTest(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse
    ): String? {
        return service.noLoggedInAccessTest(httpServletResponse)
    }


    // ----
    @Operation(
        summary = "로그인 진입 테스트 <>",
        description = "로그인 되어 있어야 진입 가능"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/for-logged-in"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun loggedInAccessTest(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): String? {
        return service.loggedInAccessTest(httpServletResponse, authorization!!)
    }


    // ----
    @Operation(
        summary = "ADMIN 권한 진입 테스트 <'ADMIN'>",
        description = "ADMIN 권한이 있어야 진입 가능"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content()],
                description = "인가되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/for-admin"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun adminAccessTest(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): String? {
        return service.adminAccessTest(httpServletResponse, authorization!!)
    }


    // ----
    @Operation(
        summary = "Developer 권한 진입 테스트 <'ADMIN' or 'Developer'>",
        description = "Developer 권한이 있어야 진입 가능"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content()],
                description = "인가되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/for-developer"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_DEVELOPER') or hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun developerAccessTest(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): String? {
        return service.developerAccessTest(httpServletResponse, authorization!!)
    }


    // ----
    @Operation(
        summary = "특정 회원의 발행된 Access 토큰 만료 처리",
        description = "특정 회원의 발행된 Access 토큰 만료 처리를 하여 Reissue 로 재검증을 하도록 만듭니다.<br>" +
                "해당 회원의 권한 변경, 계정 정지 처리 등으로 인해 발행된 토큰을 회수해야 할 때 사용하세요.<br>" +
                "단순히 만료만 시키는 것이므로 치명적인 기능을 가지진 않았지만 비밀번호를 입력해야만 동작하도록 설계하였습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : API 비밀키가 다릅니다.<br>" +
                                "2 : 존재하지 않는 회원 고유번호입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PatchMapping(
        path = ["/expire-access-token/{memberUid}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun doExpireAccessToken(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @PathVariable("memberUid")
        memberUid: Long,
        @RequestBody
        inputVo: DoExpireAccessTokenInputVo
    ) {
        service.doExpireAccessToken(httpServletResponse, memberUid, inputVo)
    }

    data class DoExpireAccessTokenInputVo(
        @Schema(
            description = "API 비밀키",
            required = true,
            example = "aadke234!@"
        )
        @JsonProperty("apiSecret")
        val apiSecret: String
    )


    // ----
    @Operation(
        summary = "계정 비밀번호 로그인",
        description = "계정 아이디 + 비밀번호를 사용하는 로그인 요청"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 입력한 id 로 가입된 회원 정보가 없습니다.<br>" +
                                "2 : 입력한 password 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/login-with-password"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun loginWithPassword(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: LoginWithPasswordInputVo
    ): LoginOutputVo? {
        return service.loginWithPassword(httpServletResponse, inputVo)
    }

    data class LoginWithPasswordInputVo(
        @Schema(
            description = "로그인 타입 (0 : 아이디, 1 : 이메일, 2 : 전화번호)",
            required = true,
            example = "1"
        )
        @JsonProperty("loginTypeCode")
        val loginTypeCode: Short,

        @Schema(
            description = "아이디 값 (0 : 홍길동, 1 : test@gmail.com, 2 : 82)000-0000-0000)",
            required = true,
            example = "test@gmail.com"
        )
        @JsonProperty("id")
        val id: String,

        @Schema(
            description = "사용할 비밀번호",
            required = true,
            example = "kkdli!!"
        )
        @JsonProperty("password")
        val password: String
    )

    data class LoginOutputVo(
        @Schema(description = "로그인 성공 정보 (이 변수가 Null 이 아니라면 lockedOutputList 가 Null 입니다.)", required = false)
        @JsonProperty("loggedInOutput")
        val loggedInOutput: LoggedInOutput?,

        @Schema(description = "계정 잠김 정보 리스트 (이 변수가 Null 이 아니라면 loggedInOutput 이 Null 입니다. 최신순 정렬)", required = false)
        @JsonProperty("lockedOutputList")
        val lockedOutputList: List<LockedOutput>?
    ) {
        @Schema(description = "계정 잠김 정보")
        data class LockedOutput(
            @Schema(description = "멤버 고유값", required = true, example = "1")
            @JsonProperty("memberUid")
            val memberUid: Long,

            @Schema(
                description = "계정 정지 시작 시간",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("lockStart")
            val lockStart: String,

            @Schema(
                description = "계정 정지 만료 시간 (이 시간이 지나기 전까지 계정 정지 상태, null 이라면 무기한 정지)",
                required = false,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("lockBefore")
            val lockBefore: String?,

            @Schema(description = "계정 정지 이유 코드(0 : 기타, 1 : 휴면계정, 2 : 패널티)", required = true, example = "1")
            @JsonProperty("lockReasonCode")
            val lockReasonCode: Int,

            @Schema(
                description = "계정 정지 이유 상세(시스템 악용 패널티, 1년 이상 미접속 휴면계정 등...)",
                required = true,
                example = "시스템 악용 패널티"
            )
            @JsonProperty("lockReason")
            val lockReason: String,
        )

        @Schema(description = "로그인 성공 정보")
        data class LoggedInOutput(
            @Schema(description = "멤버 고유값", required = true, example = "1")
            @JsonProperty("memberUid")
            val memberUid: Long,

            @Schema(description = "인증 토큰 타입", required = true, example = "Bearer")
            @JsonProperty("tokenType")
            val tokenType: String,

            @Schema(description = "엑세스 토큰", required = true, example = "kljlkjkfsdlwejoe")
            @JsonProperty("accessToken")
            val accessToken: String,

            @Schema(description = "리프레시 토큰", required = true, example = "cxfdsfpweiijewkrlerw")
            @JsonProperty("refreshToken")
            val refreshToken: String,

            @Schema(
                description = "엑세스 토큰 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("accessTokenExpireWhen")
            val accessTokenExpireWhen: String,

            @Schema(
                description = "리프레시 토큰 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("refreshTokenExpireWhen")
            val refreshTokenExpireWhen: String
        )
    }


    // ----
    @Operation(
        summary = "OAuth2 Code 로 OAuth2 AccessToken 발급",
        description = "OAuth2 Code 를 사용하여 얻은 OAuth2 AccessToken 발급"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 유효하지 않은 OAuth2 인증 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/oauth2-access-token"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun getOAuth2AccessToken(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(
            name = "oauth2TypeCode",
            description = "OAuth2 종류 코드 (1 : GOOGLE, 2 : NAVER, 3 : KAKAO)",
            example = "1"
        )
        @RequestParam("oauth2TypeCode")
        oauth2TypeCode: Int,
        @Parameter(name = "oauth2Code", description = "OAuth2 인증으로 받은 OAuth2 Code", example = "asdfeqwer1234")
        @RequestParam("oauth2Code")
        oauth2Code: String
    ): GetOAuth2AccessTokenOutputVo? {
        return service.getOAuth2AccessToken(httpServletResponse, oauth2TypeCode, oauth2Code)
    }

    data class GetOAuth2AccessTokenOutputVo(
        @Schema(
            description = "Code 로 발급받은 SNS AccessToken Type",
            required = true,
            example = "Bearer"
        )
        @JsonProperty("oAuth2AccessTokenType")
        val oAuth2AccessTokenType: String,

        @Schema(
            description = "Code 로 발급받은 SNS AccessToken",
            required = true,
            example = "abcd1234!@#$"
        )
        @JsonProperty("oAuth2AccessToken")
        val oAuth2AccessToken: String
    )


    // ----
    @Operation(
        summary = "OAuth2 로그인 (Access Token)",
        description = "OAuth2 Access Token 으로 로그인 요청"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 유효하지 않은 OAuth2 Access Token 입니다.<br>" +
                                "2 : 가입 된 회원 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/login-with-oauth2-access-token"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun loginWithOAuth2AccessToken(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: LoginWithOAuth2AccessTokenInputVo
    ): LoginOutputVo? {
        return service.loginWithOAuth2AccessToken(httpServletResponse, inputVo)
    }

    data class LoginWithOAuth2AccessTokenInputVo(
        @Schema(
            description = "OAuth2 종류 코드 (1 : GOOGLE, 2 : NAVER, 3 : KAKAO)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Int,

        @Schema(
            description = "OAuth2 인증으로 받은 TokenType + AccessToken",
            required = true,
            example = "Bearer asdfeqwer1234"
        )
        @JsonProperty("oauth2AccessToken")
        val oauth2AccessToken: String
    )


    // ----
    @Operation(
        summary = "OAuth2 로그인 (ID Token)",
        description = "OAuth2 ID Token 으로 로그인 요청"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 유효하지 않은 OAuth2 ID Token 입니다.<br>" +
                                "2 : 가입 된 회원 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/login-with-oauth2-id-token"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun loginWithOAuth2IdToken(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: LoginWithOAuth2IdTokenInputVo
    ): LoginOutputVo? {
        return service.loginWithOAuth2IdToken(httpServletResponse, inputVo)
    }

    data class LoginWithOAuth2IdTokenInputVo(
        @Schema(
            description = "OAuth2 종류 코드 (4 : Apple)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Int,

        @Schema(
            description = "OAuth2 인증으로 받은 ID Token",
            required = true,
            example = "asdfeqwer1234"
        )
        @JsonProperty("oauth2IdToken")
        val oauth2IdToken: String
    )


    // ----
    @Operation(
        summary = "로그아웃 처리 <>",
        description = "로그아웃 처리"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/logout"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun logout(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization") authorization: String?
    ) {
        service.logout(authorization!!, httpServletResponse)
    }


    // ----
    @Operation(
        summary = "토큰 재발급 <>",
        description = "엑세스 토큰 및 리프레시 토큰 재발행"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 유효하지 않은 Refresh Token 입니다.<br>" +
                                "2 : Refresh Token 이 만료되었습니다.<br>" +
                                "3 : 올바르지 않은 Access Token 입니다.<br>" +
                                "4 : 탈퇴된 회원입니다.<br>" +
                                "5 : 로그아웃 처리된 Access Token 입니다.(갱신 불가)",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/reissue"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun reissueJwt(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: ReissueJwtInputVo
    ): LoginOutputVo? {
        return service.reissueJwt(authorization, inputVo, httpServletResponse)
    }

    data class ReissueJwtInputVo(
        @Schema(description = "리프레시 토큰 (토큰 타입을 앞에 붙이기)", required = true, example = "Bearer 1sdfsadfsdafsdafsdafd")
        @JsonProperty("refreshToken")
        val refreshToken: String
    )


    // ----
    @Operation(
        summary = "멤버의 현재 발행된 모든 토큰 비활성화 (= 모든 기기에서 로그아웃) <>",
        description = "멤버의 현재 발행된 모든 토큰을 비활성화 (= 모든 기기에서 로그아웃) 하는 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/all-authorization-token"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun deleteAllJwtOfAMember(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ) {
        service.deleteAllJwtOfAMember(authorization!!, httpServletResponse)
    }


    // ----
    @Operation(
        summary = "회원 정보 가져오기 <>",
        description = "회원 정보 반환<br>" +
                "바뀔 가능성이 없는 회원 정보는 로그인시 반환되며,<br>" +
                "이 API 에서는 변경 가능성이 있는 회원 정보를 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/member-info"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun getMemberInfo(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): GetMemberInfoOutputVo? {
        return service.getMemberInfo(
            httpServletResponse,
            authorization!!
        )
    }

    data class GetMemberInfoOutputVo(
        @Schema(description = "아이디", required = true, example = "hongGilDong")
        @JsonProperty("accountId")
        val accountId: String,

        @Schema(
            description = "권한 리스트 (관리자 : ROLE_ADMIN, 개발자 : ROLE_DEVELOPER)",
            required = true,
            example = "[\"ROLE_ADMIN\", \"ROLE_DEVELOPER\"]"
        )
        @JsonProperty("roleList")
        val roleList: List<String>,

        @Schema(description = "내가 등록한 OAuth2 정보 리스트", required = true)
        @JsonProperty("myOAuth2List")
        val myOAuth2List: List<OAuth2Info>,

        @Schema(description = "내가 등록한 Profile 정보 리스트", required = true)
        @JsonProperty("myProfileList")
        val myProfileList: List<ProfileInfo>,

        @Schema(description = "내가 등록한 이메일 정보 리스트", required = true)
        @JsonProperty("myEmailList")
        val myEmailList: List<EmailInfo>,

        @Schema(description = "내가 등록한 전화번호 정보 리스트", required = true)
        @JsonProperty("myPhoneNumberList")
        val myPhoneNumberList: List<PhoneNumberInfo>,

        @Schema(
            description = "계정 로그인 비밀번호 설정 Null 여부 (OAuth2 만으로 회원가입한 경우는 비밀번호가 없으므로 true)",
            required = true,
            example = "true"
        )
        @JsonProperty("authPasswordIsNull")
        val authPasswordIsNull: Boolean
    ) {
        @Schema(description = "OAuth2 정보")
        data class OAuth2Info(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(
                description = "OAuth2 (1 : Google, 2 : Naver, 3 : Kakao, 4 : Apple)",
                required = true,
                example = "1"
            )
            @JsonProperty("oauth2TypeCode")
            val oauth2TypeCode: Int,
            @Schema(description = "oAuth2 고유값 아이디", required = true, example = "asdf1234")
            @JsonProperty("oauth2Id")
            val oauth2Id: String
        )

        @Schema(description = "Profile 정보")
        data class ProfileInfo(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(description = "프로필 이미지 Full URL", required = true, example = "https://profile-image.com/1.jpg")
            @JsonProperty("imageFullUrl")
            val imageFullUrl: String,
            @Schema(description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true, example = "1")
            @JsonProperty("priority")
            val priority: Int
        )

        @Schema(description = "이메일 정보")
        data class EmailInfo(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(description = "이메일 주소", required = true, example = "test@gmail.com")
            @JsonProperty("emailAddress")
            val emailAddress: String,
            @Schema(description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true, example = "1")
            @JsonProperty("priority")
            val priority: Int
        )

        @Schema(description = "전화번호 정보")
        data class PhoneNumberInfo(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(description = "전화번호", required = true, example = "82)010-6222-6461")
            @JsonProperty("phoneNumber")
            val phoneNumber: String,
            @Schema(description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true, example = "1")
            @JsonProperty("priority")
            val priority: Int
        )
    }


    // ----
    @Operation(
        summary = "아이디 중복 검사",
        description = "아이디 중복 여부 반환"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @GetMapping(
        path = ["/id-duplicate-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkIdDuplicate(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "id", description = "중복 검사 아이디", example = "hongGilDong")
        @RequestParam("id")
        id: String
    ): CheckIdDuplicateOutputVo? {
        return service.checkIdDuplicate(
            httpServletResponse,
            id
        )
    }

    data class CheckIdDuplicateOutputVo(
        @Schema(description = "중복여부", required = true, example = "false")
        @JsonProperty("duplicated")
        val duplicated: Boolean
    )


    // ----
    @Operation(
        summary = "아이디 수정하기 <>",
        description = "아이디 수정하기"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 동일한 아이디를 사용하는 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PatchMapping(
        path = ["/my/profile/id"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun updateId(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "id", description = "아이디", example = "mrHong")
        @RequestParam(value = "id")
        id: String
    ) {
        service.updateId(
            httpServletResponse,
            authorization!!,
            id
        )
    }


    // ----
    @Operation(
        summary = "테스트 회원 회원가입",
        description = "테스트용으로, 입력받은 정보를 가지고 회원가입 처리"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : API 비밀키가 다릅니다.<br>" +
                                "2 : 이미 동일한 아이디로 가입된 회원이 존재합니다.<br>" +
                                "3 : 이미 동일한 이메일로 가입된 회원이 존재합니다.<br>" +
                                "4 : 이미 동일한 전화번호로 가입된 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-for-test"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun joinTheMembershipForTest(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter
        inputVo: JoinTheMembershipForTestInputVo
    ) {
        service.joinTheMembershipForTest(httpServletResponse, inputVo)
    }

    data class JoinTheMembershipForTestInputVo(
        @Schema(
            description = "API 비밀키",
            required = true,
            example = "aadke234!@"
        )
        @JsonProperty("apiSecret")
        val apiSecret: String,

        @Schema(
            description = "아이디 - 이메일",
            required = false,
            example = "test@gmail.com"
        )
        @JsonProperty("email")
        val email: String?,

        @Schema(
            description = "아이디 - 전화번호(국가번호 + 전화번호)",
            required = false,
            example = "82)010-0000-0000"
        )
        @JsonProperty("phoneNumber")
        val phoneNumber: String?,

        @Schema(
            description = "계정 아이디",
            required = true,
            example = "hongGilDong"
        )
        @JsonProperty("id")
        val id: String,

        @Schema(
            description = "사용할 비밀번호",
            required = true,
            example = "kkdli!!"
        )
        @JsonProperty("password")
        val password: String,

        @Schema(description = "프로필 이미지 파일", required = false)
        @JsonProperty("profileImageFile")
        val profileImageFile: MultipartFile?
    )


    // ----
    @Operation(
        summary = "이메일 회원가입 본인 인증 이메일 발송",
        description = "이메일 회원가입시 본인 이메일 확인 메일 발송<br>" +
                "발송 후 10분 후 만료됨"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 동일한 이메일을 사용하는 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-email-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun sendEmailVerificationForJoin(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: SendEmailVerificationForJoinInputVo
    ): SendEmailVerificationForJoinOutputVo? {
        return service.sendEmailVerificationForJoin(httpServletResponse, inputVo)
    }

    data class SendEmailVerificationForJoinInputVo(
        @Schema(description = "수신 이메일", required = true, example = "test@gmail.com")
        @JsonProperty("email")
        val email: String
    )

    data class SendEmailVerificationForJoinOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,
        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("verificationExpireWhen")
        val verificationExpireWhen: String
    )


    // ----
    @Operation(
        summary = "이메일 회원가입 본인 확인 이메일에서 받은 코드 검증하기",
        description = "이메일 회원가입시 본인 이메일에 보내진 코드를 입력하여 일치 결과 확인"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/join-the-membership-email-verification-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkEmailVerificationForJoin(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "verificationUid", description = "검증 고유값", example = "1")
        @RequestParam("verificationUid")
        verificationUid: Long,
        @Parameter(name = "email", description = "확인 이메일", example = "test@gmail.com")
        @RequestParam("email")
        email: String,
        @Parameter(name = "verificationCode", description = "확인 이메일에 전송된 코드", example = "123456")
        @RequestParam("verificationCode")
        verificationCode: String
    ) {
        service.checkEmailVerificationForJoin(httpServletResponse, verificationUid, email, verificationCode)
    }


    // ----
    @Operation(
        summary = "이메일 회원가입",
        description = "이메일 회원가입 처리"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 이미 동일한 이메일로 가입된 회원이 존재합니다.<br>" +
                                "5 : 이미 동일한 아이디로 가입된 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-with-email"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun joinTheMembershipWithEmail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter
        inputVo: JoinTheMembershipWithEmailInputVo
    ) {
        service.joinTheMembershipWithEmail(httpServletResponse, inputVo)
    }

    data class JoinTheMembershipWithEmailInputVo(
        @Schema(
            description = "아이디 - 이메일",
            required = true,
            example = "test@gmail.com"
        )
        @JsonProperty("email")
        val email: String,

        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "사용할 비밀번호",
            required = true,
            example = "kkdli!!"
        )
        @JsonProperty("password")
        val password: String,

        @Schema(
            description = "계정 아이디",
            required = true,
            example = "hongGilDong"
        )
        @JsonProperty("id")
        val id: String,

        @Schema(
            description = "이메일 검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String,

        @Schema(description = "프로필 이미지 파일", required = false)
        @JsonProperty("profileImageFile")
        val profileImageFile: MultipartFile?
    )


    // ----
    @Operation(
        summary = "전화번호 회원가입 본인 인증 문자 발송",
        description = "전화번호 회원가입시 본인 전화번호 확인 문자 발송<br>" +
                "발송 후 10분 후 만료됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이미 동일한 전화번호로 가입된 회원이 존재합니다.<br>" +
                                "2 : 설명2",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-phone-number-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun sendPhoneVerificationForJoin(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: SendPhoneVerificationForJoinInputVo
    ): SendPhoneVerificationForJoinOutputVo? {
        return service.sendPhoneVerificationForJoin(httpServletResponse, inputVo)
    }

    data class SendPhoneVerificationForJoinInputVo(
        @Schema(description = "인증 문자 수신 전화번호(국가번호 + 전화번호)", required = true, example = "82)010-0000-0000")
        @JsonProperty("phoneNumber")
        val phoneNumber: String
    )

    data class SendPhoneVerificationForJoinOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("verificationExpireWhen")
        val verificationExpireWhen: String
    )


    // ----
    @Operation(
        summary = "전화번호 회원가입 본인 확인 문자에서 받은 코드 검증하기",
        description = "전화번호 회원가입시 본인 전화번호에 보내진 코드를 입력하여 일치 결과 확인"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 전화번호 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 전화번호 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/join-the-membership-phone-number-verification-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkPhoneVerificationForJoin(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "verificationUid", description = "검증 고유값", example = "1")
        @RequestParam("verificationUid")
        verificationUid: Long,
        @Parameter(name = "phoneNumber", description = "인증 문자 수신 전화번호(국가번호 + 전화번호)", example = "82)010-0000-0000")
        @RequestParam("phoneNumber")
        phoneNumber: String,
        @Parameter(name = "verificationCode", description = "확인 문자에 전송된 코드", example = "123456")
        @RequestParam("verificationCode")
        verificationCode: String
    ) {
        service.checkPhoneVerificationForJoin(httpServletResponse, verificationUid, phoneNumber, verificationCode)
    }


    // ----
    @Operation(
        summary = "전화번호 회원가입",
        description = "전화번호 회원가입 처리"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 전화번호 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 전화번호 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 이미 동일한 전화번호로 가입된 회원이 존재합니다.<br>" +
                                "5 : 이미 동일한 아이디로 가입된 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-with-phone-number"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun joinTheMembershipWithPhoneNumber(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter
        inputVo: JoinTheMembershipWithPhoneNumberInputVo
    ) {
        service.joinTheMembershipWithPhoneNumber(httpServletResponse, inputVo)
    }

    data class JoinTheMembershipWithPhoneNumberInputVo(
        @Schema(
            description = "아이디 - 전화번호(국가번호 + 전화번호)",
            required = true,
            example = "82)010-0000-0000"
        )
        @JsonProperty("phoneNumber")
        val phoneNumber: String,

        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "사용할 비밀번호",
            required = true,
            example = "kkdli!!"
        )
        @JsonProperty("password")
        val password: String,

        @Schema(
            description = "계정 아이디",
            required = true,
            example = "hongGilDong"
        )
        @JsonProperty("id")
        val id: String,

        @Schema(
            description = "문자 검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String,

        @Schema(description = "프로필 이미지 파일", required = false)
        @JsonProperty("profileImageFile")
        val profileImageFile: MultipartFile?
    )


    // ----
    @Operation(
        summary = "OAuth2 AccessToken 으로 회원가입 검증",
        description = "OAuth2 AccessToken 으로 회원가입 검증"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : Oauth2 AccessToken 이 유효하지 않습니다.<br>" +
                                "2 : 이미 동일한 Oauth2 AccessToken 으로 가입된 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-oauth2-access-token-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkOauth2AccessTokenVerificationForJoin(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: CheckOauth2AccessTokenVerificationForJoinInputVo
    ): CheckOauth2AccessTokenVerificationForJoinOutputVo? {
        return service.checkOauth2AccessTokenVerificationForJoin(httpServletResponse, inputVo)
    }

    data class CheckOauth2AccessTokenVerificationForJoinInputVo(
        @Schema(
            description = "OAuth2 종류 코드 (1 : GOOGLE, 2 : NAVER, 3 : KAKAO)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Int,

        @Schema(
            description = "OAuth2 인증으로 받은 OAuth2 TokenType + AccessToken",
            required = true,
            example = "Bearer asdfeqwer1234"
        )
        @JsonProperty("oauth2AccessToken")
        val oauth2AccessToken: String
    )

    data class CheckOauth2AccessTokenVerificationForJoinOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "OAuth2 가입시 검증에 사용할 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("oauth2VerificationCode")
        val oauth2VerificationCode: String,

        @Schema(
            description = "가입에 사용할 OAuth2 고유 아이디",
            required = true,
            example = "abcd1234"
        )
        @JsonProperty("oauth2Id")
        val oauth2Id: String,

        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("expireWhen")
        val expireWhen: String
    )


    // ----
    @Operation(
        summary = "OAuth2 IdToken 으로 회원가입 검증",
        description = "OAuth2 IdToken 으로 회원가입 검증"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : OAuth2 IdToken 이 유효하지 않습니다.<br>" +
                                "2 : 이미 동일한 OAuth2 IdToken 으로 가입된 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-oauth2-id-token-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkOauth2IdTokenVerificationForJoin(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: CheckOauth2IdTokenVerificationForJoinInputVo
    ): CheckOauth2IdTokenVerificationForJoinOutputVo? {
        return service.checkOauth2IdTokenVerificationForJoin(httpServletResponse, inputVo)
    }

    data class CheckOauth2IdTokenVerificationForJoinInputVo(
        @Schema(
            description = "OAuth2 종류 코드 (4 : Apple)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Int,

        @Schema(
            description = "OAuth2 인증으로 받은 OAuth2 IdToken",
            required = true,
            example = "asdfeqwer1234"
        )
        @JsonProperty("oauth2IdToken")
        val oauth2IdToken: String
    )

    data class CheckOauth2IdTokenVerificationForJoinOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "OAuth2 가입시 검증에 사용할 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("oauth2VerificationCode")
        val oauth2VerificationCode: String,

        @Schema(
            description = "가입에 사용할 OAuth2 고유 아이디",
            required = true,
            example = "abcd1234"
        )
        @JsonProperty("oauth2Id")
        val oauth2Id: String,

        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("expireWhen")
        val expireWhen: String
    )


    // ----
    @Operation(
        summary = "OAuth2 회원가입",
        description = "OAuth2 회원가입 처리"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : OAuth2 검증 요청을 보낸적이 없습니다.<br>" +
                                "2 : OAuth2 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 이미 동일한 OAuth2 정보로 가입된 회원이 존재합니다.<br>" +
                                "5 : 이미 동일한 아이디로 가입된 회원이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/join-the-membership-with-oauth2"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun joinTheMembershipWithOauth2(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter
        inputVo: JoinTheMembershipWithOauth2InputVo
    ) {
        service.joinTheMembershipWithOauth2(httpServletResponse, inputVo)
    }

    data class JoinTheMembershipWithOauth2InputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "가입에 사용할 OAuth2 고유 아이디",
            required = true,
            example = "abcd1234"
        )
        @JsonProperty("oauth2Id")
        val oauth2Id: String,

        @Schema(
            description = "OAuth2 종류 코드 (1 : Google, 2 : Naver, 3 : Kakao, 4 : Apple)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Short,

        @Schema(
            description = "계정 아이디",
            required = true,
            example = "hongGilDong"
        )
        @JsonProperty("id")
        val id: String,

        @Schema(
            description = "oauth2Id 검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String,

        @Schema(description = "프로필 이미지 파일", required = false)
        @JsonProperty("profileImageFile")
        val profileImageFile: MultipartFile?
    )


    // ----
    @Operation(
        summary = "계정 비밀번호 변경 <>",
        description = "계정 비밀번호 변경<br>" +
                "변경 완료된 후, 기존 모든 인증/인가 토큰이 비활성화(로그아웃) 됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 기존 비밀번호가 일치하지 않습니다.<br>" +
                                "2 : 비밀번호를 null 로 만들려고 할 때, 이외에 로그인할 수단이 없으므로 비밀번호 제거가 불가능합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/change-account-password"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun updateAccountPassword(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: UpdateAccountPasswordInputVo
    ) {
        service.updateAccountPassword(httpServletResponse, authorization!!, inputVo)
    }

    data class UpdateAccountPasswordInputVo(
        @Schema(description = "기존 계정 로그인용 비밀번호(기존 비밀번호가 없다면 null)", required = false, example = "kkdli!!")
        @JsonProperty("oldPassword")
        val oldPassword: String?,

        @Schema(description = "새 계정 로그인용 비밀번호(비밀번호를 없애려면 null)", required = false, example = "fddsd##")
        @JsonProperty("newPassword")
        val newPassword: String?
    )


    // ----
    @Operation(
        summary = "이메일 비밀번호 찾기 본인 인증 이메일 발송",
        description = "이메일 비밀번호 찾기 본인 이메일 확인 메일 발송<br>" +
                "발송 후 10분 후 만료됨"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 해당 이메일로 가입된 회원 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/find-password-email-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun sendEmailVerificationForFindPassword(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: SendEmailVerificationForFindPasswordInputVo
    ): SendEmailVerificationForFindPasswordOutputVo? {
        return service.sendEmailVerificationForFindPassword(httpServletResponse, inputVo)
    }

    data class SendEmailVerificationForFindPasswordInputVo(
        @Schema(description = "수신 이메일", required = true, example = "test@gmail.com")
        @JsonProperty("email")
        val email: String
    )

    data class SendEmailVerificationForFindPasswordOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,
        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("verificationExpireWhen")
        val verificationExpireWhen: String
    )


    // ----
    @Operation(
        summary = "이메일 비밀번호 찾기 본인 확인 이메일에서 받은 코드 검증하기",
        description = "이메일 비밀번호 찾기 시 본인 이메일에 보내진 코드를 입력하여 일치 결과 확인"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/find-password-email-verification-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkEmailVerificationForFindPassword(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "verificationUid", description = "검증 고유값", example = "1")
        @RequestParam("verificationUid")
        verificationUid: Long,
        @Parameter(name = "email", description = "확인 이메일", example = "test@gmail.com")
        @RequestParam("email")
        email: String,
        @Parameter(name = "verificationCode", description = "확인 이메일에 전송된 코드", example = "123456")
        @RequestParam("verificationCode")
        verificationCode: String
    ) {
        service.checkEmailVerificationForFindPassword(
            httpServletResponse,
            verificationUid,
            email,
            verificationCode
        )
    }


    // ----
    @Operation(
        summary = "이메일 비밀번호 찾기 완료",
        description = "계정 비밀번호를 랜덤 값으로 변경 후 인증한 이메일로 발송<br>" +
                "변경 완료된 후, 기존 모든 인증/인가 토큰이 비활성화(로그아웃) 됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 해당 이메일로 가입한 회원 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/find-password-with-email"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun findPasswordWithEmail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: FindPasswordWithEmailInputVo
    ) {
        service.findPasswordWithEmail(httpServletResponse, inputVo)
    }

    data class FindPasswordWithEmailInputVo(
        @Schema(description = "비밀번호를 찾을 계정 이메일", required = true, example = "test@gmail.com")
        @JsonProperty("email")
        val email: String,

        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "이메일 검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String
    )


    // ----
    @Operation(
        summary = "전화번호 비밀번호 찾기 본인 인증 문자 발송",
        description = "전화번호 비밀번호 찾기 본인 전화번호 확인 문자 발송<br>" +
                "발송 후 10분 후 만료됨"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 해당 전화번호로 가입된 회원 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/find-password-phone-number-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun sendPhoneVerificationForFindPassword(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: SendPhoneVerificationForFindPasswordInputVo
    ): SendPhoneVerificationForFindPasswordOutputVo? {
        return service.sendPhoneVerificationForFindPassword(httpServletResponse, inputVo)
    }

    data class SendPhoneVerificationForFindPasswordInputVo(
        @Schema(description = "수신 전화번호", required = true, example = "82)000-0000-0000")
        @JsonProperty("phoneNumber")
        val phoneNumber: String
    )

    data class SendPhoneVerificationForFindPasswordOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("verificationExpireWhen")
        val verificationExpireWhen: String
    )


    // ----
    @Operation(
        summary = "전화번호 비밀번호 찾기 본인 확인 문자에서 받은 코드 검증하기",
        description = "전화번호 비밀번호 찾기 시 본인 전와번호에 보내진 코드를 입력하여 일치 결과 확인"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 전화번호 검증 요청을 보낸적이 없습니다.<br>" +
                                "2 : 전화번호 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/find-password-phone-number-verification-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun checkPhoneVerificationForFindPassword(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "verificationUid", description = "검증 고유값", example = "1")
        @RequestParam("verificationUid")
        verificationUid: Long,
        @Parameter(name = "phoneNumber", description = "수신 전화번호", example = "82)000-0000-0000")
        @RequestParam("phoneNumber")
        phoneNumber: String,
        @Parameter(name = "verificationCode", description = "확인 이메일에 전송된 코드", example = "123456")
        @RequestParam("verificationCode")
        verificationCode: String
    ) {
        service.checkPhoneVerificationForFindPassword(
            httpServletResponse,
            verificationUid,
            phoneNumber,
            verificationCode
        )
    }


    // ----
    @Operation(
        summary = "전화번호 비밀번호 찾기 완료",
        description = "계정 비밀번호를 랜덤 값으로 변경 후 인증한 전화번호로 발송<br>" +
                "변경 완료된 후, 기존 모든 인증/인가 토큰이 비활성화 됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 전화번호 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 전화번호 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 해당 전화번호로 가입된 회원 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/find-password-with-phone-number"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun findPasswordWithPhoneNumber(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @RequestBody
        inputVo: FindPasswordWithPhoneNumberInputVo
    ) {
        service.findPasswordWithPhoneNumber(httpServletResponse, inputVo)
    }

    data class FindPasswordWithPhoneNumberInputVo(
        @Schema(description = "비밀번호를 찾을 계정 전화번호", required = true, example = "82)000-0000-0000")
        @JsonProperty("phoneNumber")
        val phoneNumber: String,

        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String
    )


    // ----
    @Operation(
        summary = "내 이메일 리스트 가져오기 <>",
        description = "내 이메일 리스트 가져오기"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/my-email-addresses"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun getMyEmailList(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): GetMyEmailListOutputVo? {
        return service.getMyEmailList(httpServletResponse, authorization!!)
    }

    data class GetMyEmailListOutputVo(
        @Schema(description = "내가 등록한 이메일 리스트", required = true)
        @JsonProperty("emailInfoList")
        val emailInfoList: List<EmailInfo>
    ) {
        @Schema(description = "이메일 정보")
        data class EmailInfo(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(description = "이메일 주소", required = true, example = "test@gmail.com")
            @JsonProperty("emailAddress")
            val emailAddress: String,
            @Schema(description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true, example = "1")
            @JsonProperty("priority")
            val priority: Int
        )
    }


    // ----
    @Operation(
        summary = "내 전화번호 리스트 가져오기 <>",
        description = "내 전화번호 리스트 가져오기"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/my-phone-numbers"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun getMyPhoneNumberList(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): GetMyPhoneNumberListOutputVo? {
        return service.getMyPhoneNumberList(httpServletResponse, authorization!!)
    }

    data class GetMyPhoneNumberListOutputVo(
        @Schema(description = "내가 등록한 전화번호 리스트", required = true)
        @JsonProperty("phoneInfoList")
        val phoneInfoList: List<PhoneInfo>
    ) {
        @Schema(description = "전화번호 정보")
        data class PhoneInfo(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(description = "전화번호", required = true, example = "82)010-6222-6461")
            @JsonProperty("phoneNumber")
            val phoneNumber: String,
            @Schema(description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true, example = "1")
            @JsonProperty("priority")
            val priority: Int
        )
    }


    // ----
    @Operation(
        summary = "내 OAuth2 로그인 리스트 가져오기 <>",
        description = "내 OAuth2 로그인 리스트 가져오기"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/my-oauth2-list"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun getMyOauth2List(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): GetMyOauth2ListOutputVo? {
        return service.getMyOauth2List(httpServletResponse, authorization!!)
    }

    data class GetMyOauth2ListOutputVo(
        @Schema(description = "내가 등록한 OAuth2 정보 리스트", required = true)
        @JsonProperty("myOAuth2List")
        val myOAuth2List: List<OAuth2Info>
    ) {
        @Schema(description = "OAuth2 정보")
        data class OAuth2Info(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(
                description = "OAuth2 (1 : Google, 2 : Naver, 3 : Kakao, 4 : Apple)",
                required = true,
                example = "1"
            )
            @JsonProperty("oauth2Type")
            val oauth2Type: Int,
            @Schema(description = "oAuth2 고유값 아이디", required = true, example = "asdf1234")
            @JsonProperty("oauth2Id")
            val oauth2Id: String
        )
    }


    // ----
    @Operation(
        summary = "이메일 추가하기 본인 인증 이메일 발송 <>",
        description = "이메일 추가하기 본인 이메일 확인 메일 발송<br>" +
                "발송 후 10분 후 만료됨"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이미 사용중인 이메일입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/add-email-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun sendEmailVerificationForAddNewEmail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: SendEmailVerificationForAddNewEmailInputVo
    ): SendEmailVerificationForAddNewEmailOutputVo? {
        return service.sendEmailVerificationForAddNewEmail(httpServletResponse, inputVo, authorization!!)
    }

    data class SendEmailVerificationForAddNewEmailInputVo(
        @Schema(description = "수신 이메일", required = true, example = "test@gmail.com")
        @JsonProperty("email")
        val email: String
    )

    data class SendEmailVerificationForAddNewEmailOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("verificationExpireWhen")
        val verificationExpireWhen: String
    )


    // ----
    @Operation(
        summary = "이메일 추가하기 본인 확인 이메일에서 받은 코드 검증하기 <>",
        description = "이메일 추가하기 본인 이메일에 보내진 코드를 입력하여 일치 결과 확인"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/add-email-verification-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun checkEmailVerificationForAddNewEmail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "verificationUid", description = "검증 고유값", example = "1")
        @RequestParam("verificationUid")
        verificationUid: Long,
        @Parameter(name = "email", description = "확인 이메일", example = "test@gmail.com")
        @RequestParam("email")
        email: String,
        @Parameter(name = "verificationCode", description = "확인 이메일에 전송된 코드", example = "123456")
        @RequestParam("verificationCode")
        verificationCode: String
    ) {
        service.checkEmailVerificationForAddNewEmail(
            httpServletResponse,
            verificationUid,
            email,
            verificationCode,
            authorization!!
        )
    }


    // ----
    @Operation(
        summary = "이메일 추가하기 <>",
        description = "내 계정에 이메일 추가"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 해당 이메일로 가입된 회원 정보가 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/my-new-email"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun addNewEmail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: AddNewEmailInputVo
    ): AddNewEmailOutputVo? {
        return service.addNewEmail(httpServletResponse, inputVo, authorization!!)
    }

    data class AddNewEmailInputVo(
        @Schema(description = "추가할 이메일", required = true, example = "test@gmail.com")
        @JsonProperty("email")
        val email: String,

        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "이메일 검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String,

        @Schema(
            description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다. null 이라면 현재 가장 높은 가중치를 적용합니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("priority")
        val priority: Int?
    )

    data class AddNewEmailOutputVo(
        @Schema(description = "이메일의 고유값", required = true, example = "1")
        @JsonProperty("emailUid")
        val emailUid: Long
    )


    // ----
    @Operation(
        summary = "내 이메일 제거하기 <>",
        description = "내 계정에서 이메일 제거"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : emailUid 의 정보가 존재하지 않습니다.<br>" +
                                "2 : 제거할 수 없습니다. (이외에 로그인할 방법이 없습니다.)",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/my-email/{emailUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun deleteMyEmail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "emailUid", description = "이메일의 고유값", example = "1")
        @PathVariable("emailUid")
        emailUid: Long
    ) {
        service.deleteMyEmail(httpServletResponse, emailUid, authorization!!)
    }


    // ----
    @Operation(
        summary = "이메일 가중치 수정 <>",
        description = "내 계정 이메일 가중치 수정"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : emailUid 의 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PatchMapping(
        path = ["/my-email/{emailUid}/priority"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun patchEmailPriority(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "emailUid", description = "이메일의 고유값", example = "1")
        @PathVariable("emailUid")
        emailUid: Long,
        @RequestBody
        inputVo: PatchEmailPriorityInputVo
    ) {
        return service.patchEmailPriority(httpServletResponse, inputVo, emailUid, authorization!!)
    }

    data class PatchEmailPriorityInputVo(
        @Schema(
            description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다. null 이라면 현재 가장 높은 가중치를 적용합니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("priority")
        val priority: Int?
    )


    // ----
    @Operation(
        summary = "전화번호 추가하기 본인 인증 문자 발송 <>",
        description = "전화번호 추가하기 본인 전화번호 확인 문자 발송<br>" +
                "발송 후 10분 후 만료됨"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이미 사용중인 전화번호입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/add-phone-number-verification"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun sendPhoneVerificationForAddNewPhoneNumber(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: SendPhoneVerificationForAddNewPhoneNumberInputVo
    ): SendPhoneVerificationForAddNewPhoneNumberOutputVo? {
        return service.sendPhoneVerificationForAddNewPhoneNumber(httpServletResponse, inputVo, authorization!!)
    }

    data class SendPhoneVerificationForAddNewPhoneNumberInputVo(
        @Schema(description = "수신 전화번호", required = true, example = "82)000-0000-0000")
        @JsonProperty("phoneNumber")
        val phoneNumber: String
    )

    data class SendPhoneVerificationForAddNewPhoneNumberOutputVo(
        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "검증 만료 시간(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("verificationExpireWhen")
        val verificationExpireWhen: String
    )


    // ----
    @Operation(
        summary = "전화번호 추가하기 본인 확인 문자에서 받은 코드 검증하기 <>",
        description = "전화번호 추가하기 본인 전화번호에 보내진 코드를 입력하여 일치 결과 확인"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 전화번호 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 전화번호 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/add-phone-number-verification-check"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun checkPhoneVerificationForAddNewPhoneNumber(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "verificationUid", description = "검증 고유값", example = "1")
        @RequestParam("verificationUid")
        verificationUid: Long,
        @Parameter(name = "phoneNumber", description = "수신 전화번호", example = "82)000-0000-0000")
        @RequestParam("phoneNumber")
        phoneNumber: String,
        @Parameter(name = "verificationCode", description = "확인 문자에 전송된 코드", example = "123456")
        @RequestParam("verificationCode")
        verificationCode: String
    ) {
        service.checkPhoneVerificationForAddNewPhoneNumber(
            httpServletResponse,
            verificationUid,
            phoneNumber,
            verificationCode,
            authorization!!
        )
    }


    // ----
    @Operation(
        summary = "전화번호 추가하기 <>",
        description = "내 계정에 전화번호 추가"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 이메일 검증 요청을 보낸 적이 없습니다.<br>" +
                                "2 : 이메일 검증 요청이 만료되었습니다.<br>" +
                                "3 : verificationCode 가 일치하지 않습니다.<br>" +
                                "4 : 이미 사용중인 전화번호입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/my-new-phone-number"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun addNewPhoneNumber(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: AddNewPhoneNumberInputVo
    ): AddNewPhoneNumberOutputVo? {
        return service.addNewPhoneNumber(httpServletResponse, inputVo, authorization!!)
    }

    data class AddNewPhoneNumberInputVo(
        @Schema(description = "추가할 전화번호", required = true, example = "82)000-0000-0000")
        @JsonProperty("phoneNumber")
        val phoneNumber: String,

        @Schema(
            description = "검증 고유값",
            required = true,
            example = "1"
        )
        @JsonProperty("verificationUid")
        val verificationUid: Long,

        @Schema(
            description = "문자 검증에 사용한 코드",
            required = true,
            example = "123456"
        )
        @JsonProperty("verificationCode")
        val verificationCode: String,

        @Schema(
            description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다. null 이라면 현재 가장 높은 가중치를 적용합니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("priority")
        val priority: Int?
    )

    data class AddNewPhoneNumberOutputVo(
        @Schema(description = "전화번호의 고유값", required = true, example = "1")
        @JsonProperty("phoneUid")
        val phoneUid: Long
    )


    // ----
    @Operation(
        summary = "내 전화번호 제거하기 <>",
        description = "내 계정에서 전화번호 제거"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : phoneUid 의 정보가 없습니다.<br>" +
                                "2 : 제거할 수 없습니다. (이외에 로그인할 방법이 없습니다.)",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/my-phone-number/{phoneUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun deleteMyPhoneNumber(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "phoneUid", description = "전화번호의 고유값", example = "1")
        @PathVariable("phoneUid")
        phoneUid: Long
    ) {
        service.deleteMyPhoneNumber(httpServletResponse, phoneUid, authorization!!)
    }


    // ----
    @Operation(
        summary = "전화번호 가중치 수정 <>",
        description = "내 계정 전화번호 가중치 수정"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : phoneUid 의 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PatchMapping(
        path = ["/my-phone-number/{phoneUid}/priority"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun patchPhoneNumberPriority(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "phoneUid", description = "전화번호의 고유값", example = "1")
        @PathVariable("phoneUid")
        phoneUid: Long,
        @RequestBody
        inputVo: PatchPhoneNumberPriorityInputVo
    ) {
        return service.patchPhoneNumberPriority(httpServletResponse, inputVo, phoneUid, authorization!!)
    }

    data class PatchPhoneNumberPriorityInputVo(
        @Schema(
            description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다. null 이라면 현재 가장 높은 가중치를 적용합니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("priority")
        val priority: Int?
    )


    // ----
    @Operation(
        summary = "OAuth2 추가하기 (Access Token) <>",
        description = "내 계정에 OAuth2 Access Token 으로 인증 추가"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : OAuth2 AccessToken 이 유효하지 않습니다.<br>" +
                                "2 : 이미 사용중인 OAuth2 인증 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/my-new-oauth2-token"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun addNewOauth2WithAccessToken(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: AddNewOauth2WithAccessTokenInputVo
    ) {
        service.addNewOauth2WithAccessToken(httpServletResponse, inputVo, authorization!!)
    }

    data class AddNewOauth2WithAccessTokenInputVo(
        @Schema(
            description = "OAuth2 종류 코드 (1 : GOOGLE, 2 : NAVER, 3 : KAKAO)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Int,

        @Schema(
            description = "OAuth2 인증으로 받은 oauth2 TokenType + AccessToken",
            required = true,
            example = "Bearer asdfeqwer1234"
        )
        @JsonProperty("oauth2AccessToken")
        val oauth2AccessToken: String
    )


    // ----
    @Operation(
        summary = "OAuth2 추가하기 (Id Token) <>",
        description = "내 계정에 OAuth2 Id Token 으로 인증 추가"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : OAuth2 Id Token 이 유효하지 않습니다.<br>" +
                                "2 : 이미 사용중인 OAuth2 인증 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/my-new-oauth2-id-token"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun addNewOauth2WithIdToken(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: AddNewOauth2WithIdTokenInputVo
    ) {
        service.addNewOauth2WithIdToken(httpServletResponse, inputVo, authorization!!)
    }

    data class AddNewOauth2WithIdTokenInputVo(
        @Schema(
            description = "OAuth2 종류 코드 (4 : Apple)",
            required = true,
            example = "1"
        )
        @JsonProperty("oauth2TypeCode")
        val oauth2TypeCode: Short,

        @Schema(
            description = "OAuth2 인증으로 받은 oauth2 IdToken",
            required = true,
            example = "asdfeqwer1234"
        )
        @JsonProperty("oauth2IdToken")
        val oauth2IdToken: String
    )


    // ----
    @Operation(
        summary = "내 OAuth2 제거하기 <>",
        description = "내 계정에서 OAuth2 제거"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : emailUid 의 정보가 존재하지 않습니다.<br>" +
                                "2 : 제거할 수 없습니다. (이외에 로그인할 방법이 없습니다.)",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/my-oauth2/{oAuth2Uid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun deleteMyOauth2(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "oAuth2Uid", description = "OAuth2 고유값", example = "1")
        @PathVariable("oAuth2Uid")
        oAuth2Uid: Long
    ) {
        service.deleteMyOauth2(httpServletResponse, oAuth2Uid, authorization!!)
    }


    // ----
    @Operation(
        summary = "회원탈퇴 <>",
        description = "회원탈퇴 요청<br>" +
                "탈퇴 완료 후 모든 토큰이 비활성화 됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/withdrawal"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun withdrawalMembership(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ) {
        service.withdrawalMembership(httpServletResponse, authorization!!)
    }


    // ----
    @Operation(
        summary = "내 Profile 이미지 정보 리스트 가져오기 <>",
        description = "내 Profile 이미지 정보 리스트 가져오기"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @GetMapping(
        path = ["/my-profile-list"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun getMyProfileList(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?
    ): GetMyProfileListOutputVo? {
        return service.getMyProfileList(httpServletResponse, authorization!!)
    }

    data class GetMyProfileListOutputVo(
        @Schema(description = "내가 등록한 Profile 이미지 정보 리스트", required = true)
        @JsonProperty("myProfileList")
        val myProfileList: List<ProfileInfo>
    ) {
        @Schema(description = "Profile 정보")
        data class ProfileInfo(
            @Schema(description = "행 고유값", required = true, example = "1")
            @JsonProperty("uid")
            val uid: Long,
            @Schema(description = "프로필 이미지 Full URL", required = true, example = "https://profile-image.com/1.jpg")
            @JsonProperty("imageFullUrl")
            val imageFullUrl: String,
            @Schema(description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true, example = "1")
            @JsonProperty("priority")
            val priority: Int
        )
    }


    // ----
    @Operation(
        summary = "내 프로필 삭제 <>",
        description = "내가 등록한 프로필들 중 하나를 삭제합니다.<br>" +
                "대표 프로필을 삭제했다면, 대표 프로필 설정이 Null 로 변경됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : profileUid 의 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/my-profile/{profileUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun deleteMyProfile(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "profileUid", description = "프로필의 고유값", example = "1")
        @PathVariable("profileUid")
        profileUid: Long
    ) {
        service.deleteMyProfile(authorization!!, httpServletResponse, profileUid)
    }


    // ----
    @Operation(
        summary = "내 프로필 가중치 수정 <>",
        description = "내 프로필 가중치 수정"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : profileUid 의 정보가 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PatchMapping(
        path = ["/my-profile/{profileUid}/priority"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun patchProfilePriority(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "profileUid", description = "프로필의 고유값", example = "1")
        @PathVariable("profileUid")
        profileUid: Long,
        @RequestBody
        inputVo: PatchProfilePriorityInputVo
    ) {
        return service.patchProfilePriority(httpServletResponse, inputVo, profileUid, authorization!!)
    }

    data class PatchProfilePriorityInputVo(
        @Schema(
            description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다. null 이라면 현재 가장 높은 가중치를 적용합니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("priority")
        val priority: Int?
    )


    // ----
    @Operation(
        summary = "내 프로필 이미지 추가 <>",
        description = "내 프로필 이미지 추가"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/my-profile"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun addNewProfile(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter
        inputVo: AddNewProfileInputVo
    ): AddNewProfileOutputVo? {
        return service.addNewProfile(httpServletResponse, authorization!!, inputVo)
    }

    data class AddNewProfileInputVo(
        @Schema(description = "프로필 이미지 파일", required = true)
        @JsonProperty("profileImageFile")
        val profileImageFile: MultipartFile,
        @Schema(
            description = "가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다. null 이라면 현재 가장 높은 가중치를 적용합니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("priority")
        val priority: Int?
    )

    data class AddNewProfileOutputVo(
        @Schema(description = "프로필의 고유값", required = true, example = "1")
        @JsonProperty("profileUid")
        val profileUid: Long,
        @Schema(description = "업로드한 프로필 이미지 파일 Full URL", required = true, example = "1")
        @JsonProperty("profileImageFullUrl")
        val profileImageFullUrl: String
    )


    // ----
    @Operation(
        summary = "by_product_files/member/profile 폴더에서 파일 다운받기",
        description = "프로필 이미지를 by_product_files/member/profile 위치에 저장했을 때 파일을 가져오기 위한 API 로,<br>" +
                "AWS 나 다른 Storage 서비스를 사용해도 좋습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 파일이 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/member-profile/{fileName}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    @ResponseBody
    fun downloadProfileFile(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "fileName", description = "by_product_files/member/profile 폴더 안의 파일명", example = "test.jpg")
        @PathVariable("fileName")
        fileName: String
    ): ResponseEntity<Resource>? {
        return service.downloadProfileFile(httpServletResponse, fileName)
    }


    // ----
    @Operation(
        summary = "Redis Key-Value 모두 조회 테스트",
        description = "Redis1_Service1ForceExpireAuthorizationSet 에 저장된 모든 Key-Value 를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @GetMapping(
        path = ["/service1-force-expire-authorization-set"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun selectAllRedisKeyValueSample(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse
    ): SelectAllRedisKeyValueSampleOutputVo? {
        return service.selectAllRedisKeyValueSample(
            httpServletResponse
        )
    }

    data class SelectAllRedisKeyValueSampleOutputVo(
        @Schema(description = "Key-Value 리스트", required = true)
        @JsonProperty("keyValueList")
        val keyValueList: List<KeyValueVo>,
    ) {
        @Schema(description = "Key-Value 객체")
        data class KeyValueVo(
            @Schema(description = "Key", required = true, example = "testing")
            @JsonProperty("key")
            val key: String,
            @Schema(description = "데이터 만료시간(밀리 초, -1 이라면 무한정)", required = true, example = "12000")
            @JsonProperty("expirationMs")
            val expirationMs: Long
        )
    }
}