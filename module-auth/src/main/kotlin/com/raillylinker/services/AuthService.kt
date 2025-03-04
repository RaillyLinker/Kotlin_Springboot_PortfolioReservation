package com.raillylinker.services

import com.raillylinker.configurations.SecurityConfig.AuthTokenFilterTotalAuth
import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.jpa_beans.db1_main.entities.*
import com.raillylinker.jpa_beans.db1_main.repositories.*
import com.raillylinker.redis_map_components.redis1_main.Redis1_Map_TotalAuthForceExpireAuthorizationSet
import com.raillylinker.retrofit2_classes.RepositoryNetworkRetrofit2
import com.raillylinker.controllers.AuthController
import com.raillylinker.jpa_beans.db1_main.repositories_dsl.Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory_RepositoryDsl
import com.raillylinker.kafka_components.producers.Kafka1MainProducer
import com.raillylinker.util_components.*
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class AuthService(
    // (프로젝트 실행시 사용 설정한 프로필명 (ex : dev8080, prod80, local8080, 설정 안하면 default 반환))
    @Value("\${spring.profiles.active:default}") private var activeProfile: String,
    private val authTokenFilterTotalAuth: AuthTokenFilterTotalAuth,

    private val passwordEncoder: PasswordEncoder,
    private val emailSender: EmailSender,
    private val naverSmsSenderComponent: NaverSmsSenderComponent,
    private val jwtTokenUtil: JwtTokenUtil,
    private val appleOAuthHelperUtil: AppleOAuthHelperUtil,
    private val customUtil: CustomUtil,

    // (Redis Map)
    private val redis1MapTotalAuthForceExpireAuthorizationSet: Redis1_Map_TotalAuthForceExpireAuthorizationSet,

    // (Database Repository)
    private val db1RaillyLinkerCompanyTotalAuthMemberRepository: Db1_RaillyLinkerCompany_TotalAuthMember_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberRoleRepository: Db1_RaillyLinkerCompany_TotalAuthMemberRole_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberEmailRepository: Db1_RaillyLinkerCompany_TotalAuthMemberEmail_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository: Db1_RaillyLinkerCompany_TotalAuthMemberPhone_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository: Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login_Repository,
    private val db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithPhoneVerificationRepository: Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithPhoneVerification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithEmailVerificationRepository: Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithEmailVerification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository: Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithOauth2Verification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthFindPwWithPhoneVerificationRepository: Db1_RaillyLinkerCompany_TotalAuthFindPwWithPhoneVerification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthFindPwWithEmailVerificationRepository: Db1_RaillyLinkerCompany_TotalAuthFindPwWithEmailVerification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthAddEmailVerificationRepository: Db1_RaillyLinkerCompany_TotalAuthAddEmailVerification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthAddPhoneVerificationRepository: Db1_RaillyLinkerCompany_TotalAuthAddPhoneVerification_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberProfileRepository: Db1_RaillyLinkerCompany_TotalAuthMemberProfile_Repository,
    private val db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository: Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepository: Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory_Repository,

    private val db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepositoryDsl: Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory_RepositoryDsl,

    private val kafka1MainProducer: Kafka1MainProducer
) {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    // Retrofit2 요청 객체
    val networkRetrofit2: RepositoryNetworkRetrofit2 = RepositoryNetworkRetrofit2.getInstance()

    // (현 프로젝트 동작 서버의 외부 접속 주소)
    // 프로필 이미지 로컬 저장 및 다운로드 주소 지정을 위해 필요
    // !!!프로필별 접속 주소 설정하기!!
    // ex : http://127.0.0.1:8080
    private val externalAccessAddress: String
        get() {
            return when (activeProfile) {
                "prod80" -> {
                    "http://127.0.0.1"
                }

                "dev8080" -> {
                    "http://127.0.0.1:11000"
                }

                else -> {
                    "http://127.0.0.1:11000"
                }
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (비 로그인 접속 테스트)
    fun noLoggedInAccessTest(httpServletResponse: HttpServletResponse): String? {
        httpServletResponse.status = HttpStatus.OK.value()
        return externalAccessAddress
    }


    // ----
    // (로그인 진입 테스트 <>)
    fun loggedInAccessTest(httpServletResponse: HttpServletResponse, authorization: String): String? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return "Member No.$memberUid : Test Success"
    }


    // ----
    // (ADMIN 권한 진입 테스트 <'ADMIN'>)
    fun adminAccessTest(httpServletResponse: HttpServletResponse, authorization: String): String? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return "Member No.$memberUid : Test Success"
    }


    // ----
    // (Developer 권한 진입 테스트 <'ADMIN' or 'Developer'>)
    fun developerAccessTest(httpServletResponse: HttpServletResponse, authorization: String): String? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return "Member No.$memberUid : Test Success"
    }


    // ----
    // (특정 회원의 발행된 Access 토큰 만료 처리)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun doExpireAccessToken(
        httpServletResponse: HttpServletResponse,
        memberUid: Long,
        inputVo: AuthController.DoExpireAccessTokenInputVo
    ) {
        if (inputVo.apiSecret != "aadke234!@") {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        val memberEntity =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")

        if (memberEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        } else {
            val tokenEntityList =
                db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findAllByTotalAuthMemberAndAccessTokenExpireWhenAfterAndRowDeleteDateStr(
                    memberEntity,
                    LocalDateTime.now(),
                    "/"
                )
            for (tokenEntity in tokenEntityList) {
                tokenEntity.logoutDate = LocalDateTime.now()
                db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenEntity)

                val tokenType = tokenEntity.tokenType
                val accessToken = tokenEntity.accessToken

                val accessTokenExpireRemainSeconds = when (tokenType) {
                    "Bearer" -> {
                        jwtTokenUtil.getRemainSeconds(accessToken)
                    }

                    else -> {
                        null
                    }
                }

                // 강제 만료 정보에 입력하기
                try {
                    redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                        "${tokenType}_${accessToken}",
                        Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                        accessTokenExpireRemainSeconds!! * 1000
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

        httpServletResponse.status = HttpStatus.OK.value()
        return
    }


    // ----
    // (계정 비밀번호 로그인)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun loginWithPassword(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.LoginWithPasswordInputVo
    ): AuthController.LoginOutputVo? {
        val memberData: Db1_RaillyLinkerCompany_TotalAuthMember
        when (inputVo.loginTypeCode.toInt()) {
            0 -> { // 아이디
                // (정보 검증 로직 수행)
                val member = db1RaillyLinkerCompanyTotalAuthMemberRepository.findByAccountIdAndRowDeleteDateStr(
                    inputVo.id,
                    "/"
                )

                if (member == null) { // 가입된 회원이 없음
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }
                memberData = member
            }

            1 -> { // 이메일
                // (정보 검증 로직 수행)
                val memberEmail =
                    db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findByEmailAddressAndRowDeleteDateStr(
                        inputVo.id,
                        "/"
                    )

                if (memberEmail == null) { // 가입된 회원이 없음
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }
                memberData = memberEmail.totalAuthMember
            }

            2 -> { // 전화번호
                // (정보 검증 로직 수행)
                val memberPhone =
                    db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findByPhoneNumberAndRowDeleteDateStr(
                        inputVo.id,
                        "/"
                    )

                if (memberPhone == null) { // 가입된 회원이 없음
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }
                memberData = memberPhone.totalAuthMember
            }

            else -> {
                classLogger.info("loginTypeCode ${inputVo.loginTypeCode} Not Supported")
                httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
                return null
            }
        }

        if (memberData.accountPassword == null || // 페스워드는 아직 만들지 않음
            !passwordEncoder.matches(inputVo.password, memberData.accountPassword!!) // 패스워드 불일치
        ) {
            // 두 상황 모두 비밀번호 찾기를 하면 해결이 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 계정 정지 검증
        val lockList =
            db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepositoryDsl.findAllNowActivateMemberLockInfo(
                memberData.uid!!,
                LocalDateTime.now()
            )
        if (lockList.isNotEmpty()) {
            // 계정 정지 당한 상황
            val lockedOutputList: MutableList<AuthController.LoginOutputVo.LockedOutput> =
                mutableListOf()
            for (lockInfo in lockList) {
                lockedOutputList.add(
                    AuthController.LoginOutputVo.LockedOutput(
                        memberData.uid!!,
                        lockInfo.lockStart.atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                        if (lockInfo.lockBefore == null) {
                            null
                        } else {
                            lockInfo.lockBefore.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                        },
                        lockInfo.lockReasonCode.toInt(),
                        lockInfo.lockReason
                    )
                )
            }

            httpServletResponse.status = HttpStatus.OK.value()
            return AuthController.LoginOutputVo(
                null,
                lockedOutputList
            )
        }

        // 멤버의 권한 리스트를 조회 후 반환
        val memberRoleList =
            db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )
        val roleList: ArrayList<String> = arrayListOf()
        for (userRole in memberRoleList) {
            roleList.add(userRole.role)
        }

        // (토큰 생성 로직 수행)
        // 멤버 고유번호로 엑세스 토큰 생성
        val jwtAccessToken = jwtTokenUtil.generateAccessToken(
            memberData.uid!!,
            authTokenFilterTotalAuth.authJwtAccessTokenExpirationTimeSec,
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
            authTokenFilterTotalAuth.authJwtIssuer,
            authTokenFilterTotalAuth.authJwtSecretKeyString,
            roleList
        )

        val accessTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(jwtAccessToken)

        // 액세스 토큰의 리프레시 토큰 생성 및 DB 저장 = 액세스 토큰에 대한 리프레시 토큰은 1개 혹은 0개
        val jwtRefreshToken = jwtTokenUtil.generateRefreshToken(
            memberData.uid!!,
            authTokenFilterTotalAuth.authJwtRefreshTokenExpirationTimeSec,
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
            authTokenFilterTotalAuth.authJwtIssuer,
            authTokenFilterTotalAuth.authJwtSecretKeyString
        )

        val refreshTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(jwtRefreshToken)

        // 로그인 정보 저장
        db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory(
                memberData,
                "Bearer",
                LocalDateTime.now(),
                jwtAccessToken,
                accessTokenExpireWhen,
                jwtRefreshToken,
                refreshTokenExpireWhen,
                null
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.LoginOutputVo(
            AuthController.LoginOutputVo.LoggedInOutput(
                memberData.uid!!,
                "Bearer",
                jwtAccessToken,
                jwtRefreshToken,
                accessTokenExpireWhen.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                refreshTokenExpireWhen.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            ),
            null
        )
    }


    // ----
    // (OAuth2 Code 로 OAuth2 AccessToken 발급)
    fun getOAuth2AccessToken(
        httpServletResponse: HttpServletResponse,
        oauth2TypeCode: Int,
        oauth2Code: String
    ): AuthController.GetOAuth2AccessTokenOutputVo? {
        val snsAccessTokenType: String
        val snsAccessToken: String

        // !!!OAuth2 ClientId!!
        val clientId = "TODO"

        // !!!OAuth2 clientSecret!!
        val clientSecret = "TODO"

        // !!!OAuth2 로그인할때 사용한 Redirect Uri!!
        val redirectUri = "TODO"

        // (정보 검증 로직 수행)
        when (oauth2TypeCode) {
            1 -> { // GOOGLE
                // Access Token 가져오기
                val atResponse = networkRetrofit2.accountsGoogleComRequestApi.postOOauth2Token(
                    oauth2Code,
                    clientId,
                    clientSecret,
                    "authorization_code",
                    redirectUri
                ).execute()

                // code 사용 결과 검증
                if (atResponse.code() != 200 ||
                    atResponse.body() == null ||
                    atResponse.body()!!.accessToken == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsAccessTokenType = atResponse.body()!!.tokenType!!
                snsAccessToken = atResponse.body()!!.accessToken!!
            }

            2 -> { // NAVER
                // !!!OAuth2 로그인시 사용한 State!!
                val state = "TODO"

                // Access Token 가져오기
                val atResponse = networkRetrofit2.nidNaverComRequestApi.getOAuth2Dot0Token(
                    "authorization_code",
                    clientId,
                    clientSecret,
                    redirectUri,
                    oauth2Code,
                    state
                ).execute()

                // code 사용 결과 검증
                if (atResponse.code() != 200 ||
                    atResponse.body() == null ||
                    atResponse.body()!!.accessToken == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsAccessTokenType = atResponse.body()!!.tokenType!!
                snsAccessToken = atResponse.body()!!.accessToken!!
            }

            3 -> { // KAKAO
                // Access Token 가져오기
                val atResponse = networkRetrofit2.kauthKakaoComRequestApi.postOOauthToken(
                    "authorization_code",
                    clientId,
                    clientSecret,
                    redirectUri,
                    oauth2Code
                ).execute()

                // code 사용 결과 검증
                if (atResponse.code() != 200 ||
                    atResponse.body() == null ||
                    atResponse.body()!!.accessToken == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsAccessTokenType = atResponse.body()!!.tokenType!!
                snsAccessToken = atResponse.body()!!.accessToken!!
            }

            else -> {
                classLogger.info("SNS Login Type $oauth2TypeCode Not Supported")
                httpServletResponse.status = 400
                return null
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.GetOAuth2AccessTokenOutputVo(
            snsAccessTokenType,
            snsAccessToken
        )
    }


    // ----
    // (OAuth2 로그인 (Access Token))
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun loginWithOAuth2AccessToken(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.LoginWithOAuth2AccessTokenInputVo
    ): AuthController.LoginOutputVo? {
        val snsOauth2: Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login?

        // (정보 검증 로직 수행)
        when (inputVo.oauth2TypeCode) {
            1 -> { // GOOGLE
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.wwwGoogleapisComRequestApi.getOauth2V1UserInfo(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsOauth2 =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        1,
                        response.body()!!.id!!,
                        "/"
                    )
            }

            2 -> { // NAVER
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.openapiNaverComRequestApi.getV1NidMe(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsOauth2 =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        2,
                        response.body()!!.response.id,
                        "/"
                    )
            }

            3 -> { // KAKAO
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.kapiKakaoComRequestApi.getV2UserMe(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsOauth2 =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        3,
                        response.body()!!.id.toString(),
                        "/"
                    )
            }

            else -> {
                classLogger.info("SNS Login Type ${inputVo.oauth2TypeCode} Not Supported")
                httpServletResponse.status = 400
                return null
            }
        }

        if (snsOauth2 == null) { // 가입된 회원이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 계정 정지 검증
        val lockList =
            db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepositoryDsl.findAllNowActivateMemberLockInfo(
                snsOauth2.totalAuthMember.uid!!,
                LocalDateTime.now()
            )
        if (lockList.isNotEmpty()) {
            // 계정 정지 당한 상황
            val lockedOutputList: MutableList<AuthController.LoginOutputVo.LockedOutput> =
                mutableListOf()
            for (lockInfo in lockList) {
                lockedOutputList.add(
                    AuthController.LoginOutputVo.LockedOutput(
                        snsOauth2.totalAuthMember.uid!!,
                        lockInfo.lockStart.atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                        if (lockInfo.lockBefore == null) {
                            null
                        } else {
                            lockInfo.lockBefore.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                        },
                        lockInfo.lockReasonCode.toInt(),
                        lockInfo.lockReason
                    )
                )
            }

            httpServletResponse.status = HttpStatus.OK.value()
            return AuthController.LoginOutputVo(
                null,
                lockedOutputList
            )
        }

        // 멤버의 권한 리스트를 조회 후 반환
        val memberRoleList =
            db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                snsOauth2.totalAuthMember,
                "/"
            )
        val roleList: ArrayList<String> = arrayListOf()
        for (memberRole in memberRoleList) {
            roleList.add(memberRole.role)
        }

        // (토큰 생성 로직 수행)
        // 멤버 고유번호로 엑세스 토큰 생성
        val jwtAccessToken = jwtTokenUtil.generateAccessToken(
            snsOauth2.totalAuthMember.uid!!,
            authTokenFilterTotalAuth.authJwtAccessTokenExpirationTimeSec,
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
            authTokenFilterTotalAuth.authJwtIssuer,
            authTokenFilterTotalAuth.authJwtSecretKeyString,
            roleList
        )

        val accessTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(jwtAccessToken)

        // 액세스 토큰의 리프레시 토큰 생성 및 DB 저장 = 액세스 토큰에 대한 리프레시 토큰은 1개 혹은 0개
        val jwtRefreshToken = jwtTokenUtil.generateRefreshToken(
            snsOauth2.totalAuthMember.uid!!,
            authTokenFilterTotalAuth.authJwtRefreshTokenExpirationTimeSec,
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
            authTokenFilterTotalAuth.authJwtIssuer,
            authTokenFilterTotalAuth.authJwtSecretKeyString
        )

        val refreshTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(jwtRefreshToken)

        // 로그인 정보 저장
        db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory(
                snsOauth2.totalAuthMember,
                "Bearer",
                LocalDateTime.now(),
                jwtAccessToken,
                accessTokenExpireWhen,
                jwtRefreshToken,
                refreshTokenExpireWhen,
                null
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.LoginOutputVo(
            AuthController.LoginOutputVo.LoggedInOutput(
                snsOauth2.totalAuthMember.uid!!,
                "Bearer",
                jwtAccessToken,
                jwtRefreshToken,
                accessTokenExpireWhen.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                refreshTokenExpireWhen.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            ),
            null
        )
    }


    // ----
    // (OAuth2 로그인 (ID Token))
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun loginWithOAuth2IdToken(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.LoginWithOAuth2IdTokenInputVo
    ): AuthController.LoginOutputVo? {
        val snsOauth2: Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login?

        // (정보 검증 로직 수행)
        when (inputVo.oauth2TypeCode) {
            4 -> { // APPLE
                val appleInfo = appleOAuthHelperUtil.getAppleMemberData(inputVo.oauth2IdToken)

                val loginId: String
                if (appleInfo != null) {
                    loginId = appleInfo.snsId
                } else {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                snsOauth2 =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        4,
                        loginId,
                        "/"
                    )
            }

            else -> {
                classLogger.info("SNS Login Type ${inputVo.oauth2TypeCode} Not Supported")
                httpServletResponse.status = 400
                return null
            }
        }

        if (snsOauth2 == null) { // 가입된 회원이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 계정 정지 검증
        val lockList =
            db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepositoryDsl.findAllNowActivateMemberLockInfo(
                snsOauth2.totalAuthMember.uid!!,
                LocalDateTime.now()
            )
        if (lockList.isNotEmpty()) {
            // 계정 정지 당한 상황
            val lockedOutputList: MutableList<AuthController.LoginOutputVo.LockedOutput> =
                mutableListOf()
            for (lockInfo in lockList) {
                lockedOutputList.add(
                    AuthController.LoginOutputVo.LockedOutput(
                        snsOauth2.totalAuthMember.uid!!,
                        lockInfo.lockStart.atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                        if (lockInfo.lockBefore == null) {
                            null
                        } else {
                            lockInfo.lockBefore.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                        },
                        lockInfo.lockReasonCode.toInt(),
                        lockInfo.lockReason
                    )
                )
            }

            httpServletResponse.status = HttpStatus.OK.value()
            return AuthController.LoginOutputVo(
                null,
                lockedOutputList
            )
        }

        // 멤버의 권한 리스트를 조회 후 반환
        val memberRoleList =
            db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                snsOauth2.totalAuthMember,
                "/"
            )
        val roleList: ArrayList<String> = arrayListOf()
        for (userRole in memberRoleList) {
            roleList.add(userRole.role)
        }

        // (토큰 생성 로직 수행)
        // 멤버 고유번호로 엑세스 토큰 생성
        val jwtAccessToken = jwtTokenUtil.generateAccessToken(
            snsOauth2.totalAuthMember.uid!!,
            authTokenFilterTotalAuth.authJwtAccessTokenExpirationTimeSec,
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
            authTokenFilterTotalAuth.authJwtIssuer,
            authTokenFilterTotalAuth.authJwtSecretKeyString,
            roleList
        )

        val accessTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(jwtAccessToken)

        // 액세스 토큰의 리프레시 토큰 생성 및 DB 저장 = 액세스 토큰에 대한 리프레시 토큰은 1개 혹은 0개
        val jwtRefreshToken = jwtTokenUtil.generateRefreshToken(
            snsOauth2.totalAuthMember.uid!!,
            authTokenFilterTotalAuth.authJwtRefreshTokenExpirationTimeSec,
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
            authTokenFilterTotalAuth.authJwtIssuer,
            authTokenFilterTotalAuth.authJwtSecretKeyString
        )

        val refreshTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(jwtRefreshToken)

        // 로그인 정보 저장
        db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory(
                snsOauth2.totalAuthMember,
                "Bearer",
                LocalDateTime.now(),
                jwtAccessToken,
                accessTokenExpireWhen,
                jwtRefreshToken,
                refreshTokenExpireWhen,
                null
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.LoginOutputVo(
            AuthController.LoginOutputVo.LoggedInOutput(
                snsOauth2.totalAuthMember.uid!!,
                "Bearer",
                jwtAccessToken,
                jwtRefreshToken,
                accessTokenExpireWhen.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                refreshTokenExpireWhen.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            ),
            null
        )
    }


    // ----
    // (로그아웃 처리 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun logout(authorization: String, httpServletResponse: HttpServletResponse) {
        val authorizationSplit = authorization.split(" ") // ex : ["Bearer", "qwer1234"]
        val token = authorizationSplit[1].trim() // (ex : "abcd1234")

        // 해당 멤버의 토큰 발행 정보 삭제
        val tokenType = authorizationSplit[0].trim().lowercase() // (ex : "bearer")

        val tokenInfo =
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findByTokenTypeAndAccessTokenAndLogoutDateAndRowDeleteDateStr(
                tokenType,
                token,
                null,
                "/"
            )

        if (tokenInfo != null) {
            tokenInfo.logoutDate = LocalDateTime.now()
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenInfo)

            // 토큰 만료처리
            val tokenType1 = tokenInfo.tokenType
            val accessToken = tokenInfo.accessToken

            val accessTokenExpireRemainSeconds = when (tokenType1) {
                "Bearer" -> {
                    jwtTokenUtil.getRemainSeconds(accessToken)
                }

                else -> {
                    null
                }
            }

            try {
                redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                    "${tokenType1}_${accessToken}",
                    Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                    accessTokenExpireRemainSeconds!! * 1000
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (토큰 재발급 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun reissueJwt(
        authorization: String?,
        inputVo: AuthController.ReissueJwtInputVo,
        httpServletResponse: HttpServletResponse
    ): AuthController.LoginOutputVo? {
        if (authorization == null) {
            // 올바르지 않은 Authorization Token
            httpServletResponse.setHeader("api-result-code", "3")
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            return null
        }

        val authorizationSplit = authorization.split(" ") // ex : ["Bearer", "qwer1234"]
        if (authorizationSplit.size < 2) {
            // 올바르지 않은 Authorization Token
            httpServletResponse.setHeader("api-result-code", "3")
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            return null
        }

        val accessTokenType = authorizationSplit[0].trim() // (ex : "bearer")
        val accessToken = authorizationSplit[1].trim() // (ex : "abcd1234")

        // 토큰 검증
        if (accessToken == "") {
            // 액세스 토큰이 비어있음 (올바르지 않은 Authorization Token)
            httpServletResponse.setHeader("api-result-code", "3")
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            return null
        }

        when (accessTokenType.lowercase()) { // 타입 검증
            "bearer" -> { // Bearer JWT 토큰 검증
                // 토큰 문자열 해석 가능여부 확인
                val accessTokenType1: String? = try {
                    jwtTokenUtil.getTokenType(accessToken)
                } catch (_: Exception) {
                    null
                }

                if (accessTokenType1 == null || // 해석 불가능한 JWT 토큰
                    accessTokenType1.lowercase() != "jwt" || // 토큰 타입이 JWT 가 아님
                    jwtTokenUtil.getTokenUsage(
                        accessToken,
                        authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                        authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
                    ).lowercase() != "access" || // 토큰 용도가 다름
                    // 남은 시간이 최대 만료시간을 초과 (서버 기준이 변경되었을 때, 남은 시간이 더 많은 토큰을 견제하기 위한 처리)
                    jwtTokenUtil.getRemainSeconds(accessToken) > authTokenFilterTotalAuth.authJwtAccessTokenExpirationTimeSec ||
                    jwtTokenUtil.getIssuer(accessToken) != authTokenFilterTotalAuth.authJwtIssuer || // 발행인 불일치
                    !jwtTokenUtil.validateSignature(
                        accessToken,
                        authTokenFilterTotalAuth.authJwtSecretKeyString
                    ) // 시크릿 검증이 무효 = 위변조 된 토큰
                ) {
                    // 올바르지 않은 Authorization Token
                    httpServletResponse.setHeader("api-result-code", "3")
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    return null
                }

                // 토큰 검증 정상 -> 데이터베이스 현 상태 확인

                // 유저 탈퇴 여부 확인
                val accessTokenMemberUid = jwtTokenUtil.getMemberUid(
                    accessToken,
                    authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                    authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
                )
                val memberData = db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(
                    accessTokenMemberUid,
                    "/"
                )

                if (memberData == null) {
                    // 멤버 탈퇴
                    httpServletResponse.setHeader("api-result-code", "4")
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    return null
                }

                // 정지 여부 파악
                val lockList =
                    db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepositoryDsl.findAllNowActivateMemberLockInfo(
                        memberData.uid!!,
                        LocalDateTime.now()
                    )
                if (lockList.isNotEmpty()) {
                    // 계정 정지 당한 상황
                    val lockedOutputList: MutableList<AuthController.LoginOutputVo.LockedOutput> =
                        mutableListOf()
                    for (lockInfo in lockList) {
                        lockedOutputList.add(
                            AuthController.LoginOutputVo.LockedOutput(
                                memberData.uid!!,
                                lockInfo.lockStart.atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                                if (lockInfo.lockBefore == null) {
                                    null
                                } else {
                                    lockInfo.lockBefore.atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                                },
                                lockInfo.lockReasonCode.toInt(),
                                lockInfo.lockReason
                            )
                        )
                    }

                    httpServletResponse.status = HttpStatus.OK.value()
                    return AuthController.LoginOutputVo(
                        null,
                        lockedOutputList
                    )
                }

                // 로그아웃 여부 파악
                val tokenInfo =
                    db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findByTokenTypeAndAccessTokenAndLogoutDateAndRowDeleteDateStr(
                        accessTokenType,
                        accessToken,
                        null,
                        "/"
                    )

                if (tokenInfo == null) {
                    // 로그아웃된 토큰
                    httpServletResponse.setHeader("api-result-code", "5")
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    return null
                }

                // 액세스 토큰 만료 외의 인증/인가 검증 완료

                // 리플레시 토큰 검증 시작
                // 타입과 토큰을 분리
                val refreshTokenInputSplit = inputVo.refreshToken.split(" ") // ex : ["Bearer", "qwer1234"]
                if (refreshTokenInputSplit.size < 2) {
                    // 올바르지 않은 Token
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                // 타입 분리
                val tokenType = refreshTokenInputSplit[0].trim() // 첫번째 단어는 토큰 타입
                val jwtRefreshToken = refreshTokenInputSplit[1].trim() // 앞의 타입을 자르고 남은 토큰

                if (jwtRefreshToken == "") {
                    // 토큰이 비어있음 (올바르지 않은 Authorization Token)
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                when (tokenType.lowercase()) { // 타입 검증
                    "bearer" -> { // Bearer JWT 토큰 검증
                        // 토큰 문자열 해석 가능여부 확인
                        val refreshTokenType: String? = try {
                            jwtTokenUtil.getTokenType(jwtRefreshToken)
                        } catch (_: Exception) {
                            null
                        }

                        // 리프레시 토큰 검증
                        if (refreshTokenType == null || // 해석 불가능한 리프레시 토큰
                            refreshTokenType.lowercase() != "jwt" || // 토큰 타입이 JWT 가 아닐 때
                            jwtTokenUtil.getTokenUsage(
                                jwtRefreshToken,
                                authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                                authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
                            ).lowercase() != "refresh" || // 토큰 타입이 Refresh 토큰이 아닐 때
                            // 남은 시간이 최대 만료시간을 초과 (서버 기준이 변경되었을 때, 남은 시간이 더 많은 토큰을 견제하기 위한 처리)
                            jwtTokenUtil.getRemainSeconds(jwtRefreshToken) > authTokenFilterTotalAuth.authJwtRefreshTokenExpirationTimeSec ||
                            jwtTokenUtil.getIssuer(jwtRefreshToken) != authTokenFilterTotalAuth.authJwtIssuer || // 발행인이 다를 때
                            !jwtTokenUtil.validateSignature(
                                jwtRefreshToken,
                                authTokenFilterTotalAuth.authJwtSecretKeyString
                            ) || // 시크릿 검증이 유효하지 않을 때 = 위변조된 토큰
                            jwtTokenUtil.getMemberUid(
                                jwtRefreshToken,
                                authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                                authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
                            ) != accessTokenMemberUid // 리프레시 토큰의 멤버 고유번호와 액세스 토큰 멤버 고유번호가 다를시
                        ) {
                            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                            httpServletResponse.setHeader("api-result-code", "1")
                            return null
                        }

                        if (jwtTokenUtil.getRemainSeconds(jwtRefreshToken) <= 0L) {
                            // 리플레시 토큰 만료
                            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                            httpServletResponse.setHeader("api-result-code", "2")
                            return null
                        }

                        if (jwtRefreshToken != tokenInfo.refreshToken) {
                            // 건내받은 토큰이 해당 액세스 토큰의 가용 토큰과 맞지 않음
                            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                            httpServletResponse.setHeader("api-result-code", "1")
                            return null
                        }

                        // 먼저 로그아웃 처리
                        tokenInfo.logoutDate = LocalDateTime.now()
                        db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenInfo)

                        // 토큰 만료처리
                        val tokenType1 = tokenInfo.tokenType
                        val accessToken1 = tokenInfo.accessToken

                        val accessTokenExpireRemainSeconds = when (tokenType1) {
                            "Bearer" -> {
                                jwtTokenUtil.getRemainSeconds(accessToken1)
                            }

                            else -> {
                                null
                            }
                        }

                        try {
                            redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                                "${tokenType1}_${accessToken1}",
                                Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                                accessTokenExpireRemainSeconds!! * 1000
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // 멤버의 권한 리스트를 조회 후 반환
                        val memberRoleList =
                            db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                                tokenInfo.totalAuthMember,
                                "/"
                            )
                        val roleList: ArrayList<String> = arrayListOf()
                        for (userRole in memberRoleList) {
                            roleList.add(userRole.role)
                        }

                        // 새 토큰 생성 및 로그인 처리
                        val newJwtAccessToken = jwtTokenUtil.generateAccessToken(
                            accessTokenMemberUid,
                            authTokenFilterTotalAuth.authJwtAccessTokenExpirationTimeSec,
                            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
                            authTokenFilterTotalAuth.authJwtIssuer,
                            authTokenFilterTotalAuth.authJwtSecretKeyString,
                            roleList
                        )

                        val accessTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(newJwtAccessToken)

                        val newRefreshToken = jwtTokenUtil.generateRefreshToken(
                            accessTokenMemberUid,
                            authTokenFilterTotalAuth.authJwtRefreshTokenExpirationTimeSec,
                            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey,
                            authTokenFilterTotalAuth.authJwtIssuer,
                            authTokenFilterTotalAuth.authJwtSecretKeyString
                        )

                        val refreshTokenExpireWhen = jwtTokenUtil.getExpirationDateTime(newRefreshToken)

                        // 로그인 정보 저장
                        db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(
                            Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory(
                                tokenInfo.totalAuthMember,
                                "Bearer",
                                LocalDateTime.now(),
                                newJwtAccessToken,
                                accessTokenExpireWhen,
                                newRefreshToken,
                                refreshTokenExpireWhen,
                                null
                            )
                        )

                        httpServletResponse.status = HttpStatus.OK.value()
                        return AuthController.LoginOutputVo(
                            AuthController.LoginOutputVo.LoggedInOutput(
                                memberData.uid!!,
                                "Bearer",
                                newJwtAccessToken,
                                newRefreshToken,
                                accessTokenExpireWhen.atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                                refreshTokenExpireWhen.atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                            ),
                            null
                        )
                    }

                    else -> {
                        // 지원하지 않는 토큰 타입 (올바르지 않은 Authorization Token)
                        httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                        httpServletResponse.setHeader("api-result-code", "1")
                        return null
                    }
                }
            }

            else -> {
                // 올바르지 않은 Authorization Token
                httpServletResponse.setHeader("api-result-code", "3")
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                return null
            }
        }
    }


    // ----
    // (멤버의 현재 발행된 모든 토큰 비활성화 (= 모든 기기에서 로그아웃) <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteAllJwtOfAMember(authorization: String, httpServletResponse: HttpServletResponse) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // loginAccessToken 의 Iterable 가져오기
        val tokenInfoList =
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findAllByTotalAuthMemberAndLogoutDateAndRowDeleteDateStr(
                memberData,
                null,
                "/"
            )

        // 발행되었던 모든 액세스 토큰 무효화 (다른 디바이스에선 사용중 로그아웃된 것과 동일한 효과)
        for (tokenInfo in tokenInfoList) {
            tokenInfo.logoutDate = LocalDateTime.now()
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenInfo)

            // 토큰 만료처리
            val tokenType = tokenInfo.tokenType
            val accessToken = tokenInfo.accessToken

            val accessTokenExpireRemainSeconds = when (tokenType) {
                "Bearer" -> {
                    jwtTokenUtil.getRemainSeconds(accessToken)
                }

                else -> {
                    null
                }
            }

            try {
                redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                    "${tokenType}_${accessToken}",
                    Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                    accessTokenExpireRemainSeconds!! * 1000
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (회원 정보 가져오기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getMemberInfo(
        httpServletResponse: HttpServletResponse,
        authorization: String
    ): AuthController.GetMemberInfoOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 멤버의 권한 리스트를 조회 후 반환
        val memberRoleList =
            db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        val roleList: ArrayList<String> = arrayListOf()
        for (userRole in memberRoleList) {
            roleList.add(userRole.role)
        }

        val profileData =
            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )
        val myProfileList: ArrayList<AuthController.GetMemberInfoOutputVo.ProfileInfo> =
            arrayListOf()
        for (profile in profileData) {
            myProfileList.add(
                AuthController.GetMemberInfoOutputVo.ProfileInfo(
                    profile.uid!!,
                    profile.imageFullUrl,
                    profile.priority
                )
            )
        }

        val emailEntityList =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )
        val myEmailList: ArrayList<AuthController.GetMemberInfoOutputVo.EmailInfo> =
            arrayListOf()
        for (emailEntity in emailEntityList) {
            myEmailList.add(
                AuthController.GetMemberInfoOutputVo.EmailInfo(
                    emailEntity.uid!!,
                    emailEntity.emailAddress,
                    emailEntity.priority
                )
            )
        }

        val phoneEntityList =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )
        val myPhoneNumberList: ArrayList<AuthController.GetMemberInfoOutputVo.PhoneNumberInfo> =
            arrayListOf()
        for (phoneEntity in phoneEntityList) {
            myPhoneNumberList.add(
                AuthController.GetMemberInfoOutputVo.PhoneNumberInfo(
                    phoneEntity.uid!!,
                    phoneEntity.phoneNumber,
                    phoneEntity.priority
                )
            )
        }

        val oAuth2EntityList =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )
        val myOAuth2List = ArrayList<AuthController.GetMemberInfoOutputVo.OAuth2Info>()
        for (oAuth2Entity in oAuth2EntityList) {
            myOAuth2List.add(
                AuthController.GetMemberInfoOutputVo.OAuth2Info(
                    oAuth2Entity.uid!!,
                    oAuth2Entity.oauth2TypeCode.toInt(),
                    oAuth2Entity.oauth2Id
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.GetMemberInfoOutputVo(
            memberData.accountId,
            roleList,
            myOAuth2List,
            myProfileList,
            myEmailList,
            myPhoneNumberList,
            memberData.accountPassword == null
        )
    }


    // ----
    // (아이디 중복 검사)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkIdDuplicate(
        httpServletResponse: HttpServletResponse,
        id: String
    ): AuthController.CheckIdDuplicateOutputVo? {
        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.CheckIdDuplicateOutputVo(
            db1RaillyLinkerCompanyTotalAuthMemberRepository.existsByAccountIdAndRowDeleteDateStr(id.trim(), "/")
        )
    }


    // ----
    // (아이디 수정하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun updateId(httpServletResponse: HttpServletResponse, authorization: String, id: String) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        if (db1RaillyLinkerCompanyTotalAuthMemberRepository.existsByAccountIdAndRowDeleteDateStr(id, "/")) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        memberData.accountId = id
        db1RaillyLinkerCompanyTotalAuthMemberRepository.save(
            memberData
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (테스트 회원 회원가입)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun joinTheMembershipForTest(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.JoinTheMembershipForTestInputVo
    ) {
        if (inputVo.apiSecret != "aadke234!@") {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (db1RaillyLinkerCompanyTotalAuthMemberRepository.existsByAccountIdAndRowDeleteDateStr(
                inputVo.id.trim(),
                "/"
            )
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        if (inputVo.email != null) {
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByEmailAddressAndRowDeleteDateStr(
                    inputVo.email,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "3")
                return
            }
        }

        if (inputVo.phoneNumber != null) {
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByPhoneNumberAndRowDeleteDateStr(
                    inputVo.phoneNumber,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return
            }
        }

        val password = passwordEncoder.encode(inputVo.password)!! // 비밀번호 암호화

        // 회원가입
        val memberEntity = db1RaillyLinkerCompanyTotalAuthMemberRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthMember(
                inputVo.id,
                password
            )
        )

        if (inputVo.profileImageFile != null) {
            // 저장된 프로필 이미지 파일을 다운로드 할 수 있는 URL
            val savedProfileImageUrl: String

            // 프로필 이미지 파일 저장

            //----------------------------------------------------------------------------------------------------------
            // 프로필 이미지를 서버 스토리지에 저장할 때 사용하는 방식
            // 파일 저장 기본 디렉토리 경로
            val saveDirectoryPath: Path =
                Paths.get("./by_product_files/auth/member/profile").toAbsolutePath().normalize()

            val savedFileName = customUtil.multipartFileLocalSave(
                saveDirectoryPath,
                null,
                inputVo.profileImageFile
            )

            savedProfileImageUrl = "${externalAccessAddress}/auth/member-profile/$savedFileName"
            //----------------------------------------------------------------------------------------------------------

            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberProfile(
                    memberEntity,
                    savedProfileImageUrl,
                    0
                )
            )
        }

        if (inputVo.email != null) {
            // 이메일 저장
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberEmail(
                    memberEntity,
                    inputVo.email,
                    0
                )
            )
        }

        if (inputVo.phoneNumber != null) {
            // 전화번호 저장
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberPhone(
                    memberEntity,
                    inputVo.phoneNumber,
                    0
                )
            )
        }

        db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberEntity)

        httpServletResponse.status = HttpStatus.OK.value()
        return
    }


    // ----
    // (이메일 회원가입 본인 인증 이메일 발송)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun sendEmailVerificationForJoin(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.SendEmailVerificationForJoinInputVo
    ): AuthController.SendEmailVerificationForJoinOutputVo? {
        // 입력 데이터 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByEmailAddressAndRowDeleteDateStr(
                inputVo.email,
                "/"
            )

        if (memberExists) { // 기존 회원 존재
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 정보 저장 후 이메일 발송
        val verificationTimeSec: Long = 60 * 10
        val verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
        val memberRegisterEmailVerificationData =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithEmailVerificationRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithEmailVerification(
                    inputVo.email,
                    verificationCode,
                    LocalDateTime.now().plusSeconds(verificationTimeSec)
                )
            )

        emailSender.sendThymeLeafHtmlMail(
            "Springboot Mvc Project Template",
            arrayOf(inputVo.email),
            null,
            "Springboot Mvc Project Template 회원가입 - 본인 계정 확인용 이메일입니다.",
            "send_email_verification_for_join/email_verification_email",
            hashMapOf(
                Pair("verificationCode", verificationCode)
            ),
            null,
            null,
            null,
            null
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.SendEmailVerificationForJoinOutputVo(
            memberRegisterEmailVerificationData.uid!!,
            memberRegisterEmailVerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        )
    }


    // ----
    // (이메일 회원가입 본인 확인 이메일에서 받은 코드 검증하기)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkEmailVerificationForJoin(
        httpServletResponse: HttpServletResponse,
        verificationUid: Long,
        email: String,
        verificationCode: String
    ) {
        val emailVerification =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithEmailVerificationRepository.findByUidAndRowDeleteDateStr(
                verificationUid,
                "/"
            )

        if (emailVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (emailVerification.emailAddress != email) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(emailVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (emailVerification.verificationSecret == verificationCode) {
            // 코드 일치
            httpServletResponse.status = HttpStatus.OK.value()
        } else {
            // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
        }
    }


    // ----
    // (이메일 회원가입)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun joinTheMembershipWithEmail(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.JoinTheMembershipWithEmailInputVo
    ) {
        val emailVerification =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithEmailVerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (emailVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (emailVerification.emailAddress != inputVo.email) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(emailVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (emailVerification.verificationSecret == inputVo.verificationCode) { // 코드 일치
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByEmailAddressAndRowDeleteDateStr(
                    inputVo.email,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return
            }

            if (db1RaillyLinkerCompanyTotalAuthMemberRepository.existsByAccountIdAndRowDeleteDateStr(
                    inputVo.id.trim(),
                    "/"
                )
            ) {
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "5")
                return
            }

            val password = passwordEncoder.encode(inputVo.password)!! // 비밀번호 암호화

            // 회원가입
            val memberData = db1RaillyLinkerCompanyTotalAuthMemberRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMember(
                    inputVo.id,
                    password
                )
            )

            // 이메일 저장
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberEmail(
                    memberData,
                    inputVo.email,
                    0
                )
            )

            if (inputVo.profileImageFile != null) {
                // 저장된 프로필 이미지 파일을 다운로드 할 수 있는 URL
                val savedProfileImageUrl: String

                // 프로필 이미지 파일 저장

                //----------------------------------------------------------------------------------------------------------
                // 프로필 이미지를 서버 스토리지에 저장할 때 사용하는 방식
                // 파일 저장 기본 디렉토리 경로
                val saveDirectoryPath: Path =
                    Paths.get("./by_product_files/auth/member/profile").toAbsolutePath().normalize()

                val savedFileName = customUtil.multipartFileLocalSave(
                    saveDirectoryPath,
                    null,
                    inputVo.profileImageFile
                )

                savedProfileImageUrl = "${externalAccessAddress}/auth/member-profile/$savedFileName"
                //----------------------------------------------------------------------------------------------------------

                db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(
                    Db1_RaillyLinkerCompany_TotalAuthMemberProfile(
                        memberData,
                        savedProfileImageUrl,
                        0
                    )
                )
            }

            db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberData)

            // 확인 완료된 검증 요청 정보 삭제
            emailVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithEmailVerificationRepository.save(emailVerification)

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }
    }


    // ----
    // (전화번호 회원가입 본인 인증 문자 발송)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun sendPhoneVerificationForJoin(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.SendPhoneVerificationForJoinInputVo
    ): AuthController.SendPhoneVerificationForJoinOutputVo? {
        // 입력 데이터 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByPhoneNumberAndRowDeleteDateStr(
                inputVo.phoneNumber,
                "/"
            )

        if (memberExists) { // 기존 회원 존재
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 정보 저장 후 발송
        val verificationTimeSec: Long = 60 * 10
        val verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
        val memberRegisterPhoneNumberVerificationData =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithPhoneVerificationRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithPhoneVerification(
                    inputVo.phoneNumber,
                    verificationCode,
                    LocalDateTime.now().plusSeconds(verificationTimeSec)
                )
            )

        val phoneNumberSplit = inputVo.phoneNumber.split(")") // ["82", "010-0000-0000"]

        // 국가 코드 (ex : 82)
        val countryCode = phoneNumberSplit[0]

        // 전화번호 (ex : "01000000000")
        val phoneNumber = (phoneNumberSplit[1].replace("-", "")).replace(" ", "")

        val sendSmsResult = naverSmsSenderComponent.sendSms(
            NaverSmsSenderComponent.SendSmsInputVo(
                "SMS",
                countryCode,
                phoneNumber,
                "[Springboot Mvc Project Template - 회원가입] 인증번호 [${verificationCode}]"
            )
        )

        if (!sendSmsResult) {
            throw Exception()
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.SendPhoneVerificationForJoinOutputVo(
            memberRegisterPhoneNumberVerificationData.uid!!,
            memberRegisterPhoneNumberVerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        )
    }


    // ----
    // (전화번호 회원가입 본인 확인 문자에서 받은 코드 검증하기)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkPhoneVerificationForJoin(
        httpServletResponse: HttpServletResponse,
        verificationUid: Long,
        phoneNumber: String,
        verificationCode: String
    ) {
        val phoneNumberVerification =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithPhoneVerificationRepository.findByUidAndRowDeleteDateStr(
                verificationUid,
                "/"
            )

        if (phoneNumberVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (phoneNumberVerification.phoneNumber != phoneNumber) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(phoneNumberVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (phoneNumberVerification.verificationSecret == verificationCode) {
            // 코드 일치
            httpServletResponse.status = HttpStatus.OK.value()
        } else {
            // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
        }
    }


    // ----
    // (전화번호 회원가입)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun joinTheMembershipWithPhoneNumber(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.JoinTheMembershipWithPhoneNumberInputVo
    ) {
        val phoneNumberVerification =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithPhoneVerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (phoneNumberVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (phoneNumberVerification.phoneNumber != inputVo.phoneNumber) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(phoneNumberVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (phoneNumberVerification.verificationSecret == inputVo.verificationCode) { // 코드 일치
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByPhoneNumberAndRowDeleteDateStr(
                    inputVo.phoneNumber,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return
            }

            if (db1RaillyLinkerCompanyTotalAuthMemberRepository.existsByAccountIdAndRowDeleteDateStr(
                    inputVo.id.trim(),
                    "/"
                )
            ) {
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "5")
                return
            }

            val password: String = passwordEncoder.encode(inputVo.password)!! // 비밀번호 암호화

            // 회원가입
            val memberUser = db1RaillyLinkerCompanyTotalAuthMemberRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMember(
                    inputVo.id,
                    password
                )
            )

            // 전화번호 저장
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberPhone(
                    memberUser,
                    inputVo.phoneNumber,
                    0
                )
            )

            if (inputVo.profileImageFile != null) {
                // 저장된 프로필 이미지 파일을 다운로드 할 수 있는 URL
                val savedProfileImageUrl: String

                // 프로필 이미지 파일 저장

                //----------------------------------------------------------------------------------------------------------
                // 프로필 이미지를 서버 스토리지에 저장할 때 사용하는 방식
                // 파일 저장 기본 디렉토리 경로
                val saveDirectoryPath: Path =
                    Paths.get("./by_product_files/auth/member/profile").toAbsolutePath().normalize()

                val savedFileName = customUtil.multipartFileLocalSave(
                    saveDirectoryPath,
                    null,
                    inputVo.profileImageFile
                )

                savedProfileImageUrl = "${externalAccessAddress}/auth/member-profile/$savedFileName"
                //----------------------------------------------------------------------------------------------------------

                db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(
                    Db1_RaillyLinkerCompany_TotalAuthMemberProfile(
                        memberUser,
                        savedProfileImageUrl,
                        0
                    )
                )
            }

            db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberUser)

            // 확인 완료된 검증 요청 정보 삭제
            phoneNumberVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithPhoneVerificationRepository.save(
                phoneNumberVerification
            )

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }
    }


    // ----
    // (OAuth2 AccessToken 으로 회원가입 검증)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun checkOauth2AccessTokenVerificationForJoin(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.CheckOauth2AccessTokenVerificationForJoinInputVo
    ): AuthController.CheckOauth2AccessTokenVerificationForJoinOutputVo? {
        val verificationUid: Long
        val verificationCode: String
        val expireWhen: String
        val loginId: String

        val verificationTimeSec: Long = 60 * 10
        // (정보 검증 로직 수행)
        when (inputVo.oauth2TypeCode) {
            1 -> { // GOOGLE
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.wwwGoogleapisComRequestApi.getOauth2V1UserInfo(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                loginId = response.body()!!.id!!

                val memberExists =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        1,
                        loginId,
                        "/"
                    )

                if (memberExists) { // 기존 회원 존재
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
                val memberRegisterOauth2VerificationData =
                    db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository.save(
                        Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithOauth2Verification(
                            1,
                            loginId,
                            verificationCode,
                            LocalDateTime.now().plusSeconds(verificationTimeSec)
                        )
                    )

                verificationUid = memberRegisterOauth2VerificationData.uid!!

                expireWhen =
                    memberRegisterOauth2VerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            }

            2 -> { // NAVER
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.openapiNaverComRequestApi.getV1NidMe(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                loginId = response.body()!!.response.id

                val memberExists =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        2,
                        loginId,
                        "/"
                    )

                if (memberExists) { // 기존 회원 존재
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
                val memberRegisterOauth2VerificationData =
                    db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository.save(
                        Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithOauth2Verification(
                            2,
                            loginId,
                            verificationCode,
                            LocalDateTime.now().plusSeconds(verificationTimeSec)
                        )
                    )

                verificationUid = memberRegisterOauth2VerificationData.uid!!

                expireWhen =
                    memberRegisterOauth2VerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            }

            3 -> { // KAKAO TALK
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.kapiKakaoComRequestApi.getV2UserMe(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                loginId = response.body()!!.id.toString()

                val memberExists =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        3,
                        loginId,
                        "/"
                    )

                if (memberExists) { // 기존 회원 존재
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
                val memberRegisterOauth2VerificationData =
                    db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository.save(
                        Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithOauth2Verification(
                            3,
                            loginId,
                            verificationCode,
                            LocalDateTime.now().plusSeconds(verificationTimeSec)
                        )
                    )

                verificationUid = memberRegisterOauth2VerificationData.uid!!

                expireWhen =
                    memberRegisterOauth2VerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            }

            else -> {
                classLogger.info("SNS Login Type ${inputVo.oauth2TypeCode} Not Supported")
                httpServletResponse.status = 400
                return null
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.CheckOauth2AccessTokenVerificationForJoinOutputVo(
            verificationUid,
            verificationCode,
            loginId,
            expireWhen
        )
    }


    // ----
    // (OAuth2 IdToken 으로 회원가입 검증)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun checkOauth2IdTokenVerificationForJoin(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.CheckOauth2IdTokenVerificationForJoinInputVo
    ): AuthController.CheckOauth2IdTokenVerificationForJoinOutputVo? {
        val verificationUid: Long
        val verificationCode: String
        val expireWhen: String
        val loginId: String

        val verificationTimeSec: Long = 60 * 10
        // (정보 검증 로직 수행)
        when (inputVo.oauth2TypeCode) {
            4 -> { // Apple
                val appleInfo = appleOAuthHelperUtil.getAppleMemberData(inputVo.oauth2IdToken)

                if (appleInfo != null) {
                    loginId = appleInfo.snsId
                } else {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return null
                }

                val memberExists =
                    db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                        4,
                        loginId,
                        "/"
                    )

                if (memberExists) { // 기존 회원 존재
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
                val memberRegisterOauth2VerificationData =
                    db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository.save(
                        Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithOauth2Verification(
                            4,
                            loginId,
                            verificationCode,
                            LocalDateTime.now().plusSeconds(verificationTimeSec)
                        )
                    )

                verificationUid = memberRegisterOauth2VerificationData.uid!!

                expireWhen =
                    memberRegisterOauth2VerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            }

            else -> {
                classLogger.info("SNS Login Type ${inputVo.oauth2TypeCode} Not Supported")
                httpServletResponse.status = 400
                return null
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.CheckOauth2IdTokenVerificationForJoinOutputVo(
            verificationUid,
            verificationCode,
            loginId,
            expireWhen
        )
    }


    // ----
    // (OAuth2 회원가입)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun joinTheMembershipWithOauth2(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.JoinTheMembershipWithOauth2InputVo
    ) {
        // oauth2 종류 (1 : GOOGLE, 2 : NAVER, 3 : KAKAO)
        val oauth2TypeCode: Short

        when (inputVo.oauth2TypeCode.toInt()) {
            1 -> {
                oauth2TypeCode = 1
            }

            2 -> {
                oauth2TypeCode = 2
            }

            3 -> {
                oauth2TypeCode = 3
            }

            4 -> {
                oauth2TypeCode = 4
            }

            else -> {
                httpServletResponse.status = 400
                return
            }
        }

        val oauth2Verification =
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (oauth2Verification == null) { // 해당 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (oauth2Verification.oauth2TypeCode != oauth2TypeCode ||
            oauth2Verification.oauth2Id != inputVo.oauth2Id
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(oauth2Verification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (oauth2Verification.verificationSecret == inputVo.verificationCode) { // 코드 일치
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                    inputVo.oauth2TypeCode,
                    inputVo.oauth2Id,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return
            }

            if (db1RaillyLinkerCompanyTotalAuthMemberRepository.existsByAccountIdAndRowDeleteDateStr(
                    inputVo.id.trim(),
                    "/"
                )
            ) {
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "5")
                return
            }

            // 회원가입
            val memberEntity = db1RaillyLinkerCompanyTotalAuthMemberRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMember(
                    inputVo.id,
                    null
                )
            )

            // SNS OAUth2 저장
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login(
                    memberEntity,
                    inputVo.oauth2TypeCode,
                    inputVo.oauth2Id
                )
            )

            if (inputVo.profileImageFile != null) {
                // 저장된 프로필 이미지 파일을 다운로드 할 수 있는 URL
                val savedProfileImageUrl: String

                // 프로필 이미지 파일 저장

                //----------------------------------------------------------------------------------------------------------
                // 프로필 이미지를 서버 스토리지에 저장할 때 사용하는 방식
                // 파일 저장 기본 디렉토리 경로
                val saveDirectoryPath: Path =
                    Paths.get("./by_product_files/auth/member/profile").toAbsolutePath().normalize()

                val savedFileName = customUtil.multipartFileLocalSave(
                    saveDirectoryPath,
                    null,
                    inputVo.profileImageFile
                )

                savedProfileImageUrl = "${externalAccessAddress}/auth/member-profile/$savedFileName"
                //----------------------------------------------------------------------------------------------------------

                db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(
                    Db1_RaillyLinkerCompany_TotalAuthMemberProfile(
                        memberEntity,
                        savedProfileImageUrl,
                        0
                    )
                )
            }

            db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberEntity)

            // 확인 완료된 검증 요청 정보 삭제
            oauth2Verification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthJoinTheMembershipWithOauth2VerificationRepository.save(oauth2Verification)

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }
    }


    // ----
    // (계정 비밀번호 변경 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun updateAccountPassword(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        inputVo: AuthController.UpdateAccountPasswordInputVo
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        if (memberData.accountPassword == null) { // 기존 비번이 존재하지 않음
            if (inputVo.oldPassword != null) { // 비밀번호 불일치
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "1")
                return
            }
        } else { // 기존 비번 존재
            if (inputVo.oldPassword == null || !passwordEncoder.matches(
                    inputVo.oldPassword,
                    memberData.accountPassword
                )
            ) { // 비밀번호 불일치
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "1")
                return
            }
        }

        if (inputVo.newPassword == null) {
            val oAuth2EntityList =
                db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                    memberData,
                    "/"
                )

            if (oAuth2EntityList.isEmpty()) {
                // null 로 만들려고 할 때 account 외의 OAuth2 인증이 없다면 제거 불가
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "2")
                return
            }

            memberData.accountPassword = null
        } else {
            memberData.accountPassword = passwordEncoder.encode(inputVo.newPassword) // 비밀번호는 암호화
        }
        db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberData)

        // 모든 토큰 비활성화 처리
        // loginAccessToken 의 Iterable 가져오기
        val tokenInfoList =
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findAllByTotalAuthMemberAndLogoutDateAndRowDeleteDateStr(
                memberData,
                null,
                "/"
            )

        // 발행되었던 모든 액세스 토큰 무효화 (다른 디바이스에선 사용중 로그아웃된 것과 동일한 효과)
        for (tokenInfo in tokenInfoList) {
            tokenInfo.logoutDate = LocalDateTime.now()
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenInfo)

            // 토큰 만료처리
            val tokenType = tokenInfo.tokenType
            val accessToken = tokenInfo.accessToken

            val accessTokenExpireRemainSeconds = when (tokenType) {
                "Bearer" -> {
                    jwtTokenUtil.getRemainSeconds(accessToken)
                }

                else -> {
                    null
                }
            }

            try {
                redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                    "${tokenType}_${accessToken}",
                    Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                    accessTokenExpireRemainSeconds!! * 1000
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (이메일 비밀번호 찾기 본인 인증 이메일 발송)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun sendEmailVerificationForFindPassword(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.SendEmailVerificationForFindPasswordInputVo
    ): AuthController.SendEmailVerificationForFindPasswordOutputVo? {
        // 입력 데이터 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByEmailAddressAndRowDeleteDateStr(
                inputVo.email,
                "/"
            )
        if (!memberExists) { // 회원 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 정보 저장 후 이메일 발송
        val verificationTimeSec: Long = 60 * 10
        val verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
        val memberFindPasswordEmailVerificationData =
            db1RaillyLinkerCompanyTotalAuthFindPwWithEmailVerificationRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthFindPwWithEmailVerification(
                    inputVo.email,
                    verificationCode,
                    LocalDateTime.now().plusSeconds(verificationTimeSec)
                )
            )

        emailSender.sendThymeLeafHtmlMail(
            "Springboot Mvc Project Template",
            arrayOf(inputVo.email),
            null,
            "Springboot Mvc Project Template 비밀번호 찾기 - 본인 계정 확인용 이메일입니다.",
            "send_email_verification_for_find_password/find_password_email_verification_email",
            hashMapOf(
                Pair("verificationCode", verificationCode)
            ),
            null,
            null,
            null,
            null
        )

        return AuthController.SendEmailVerificationForFindPasswordOutputVo(
            memberFindPasswordEmailVerificationData.uid!!,
            memberFindPasswordEmailVerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        )
    }


    // ----
    // (이메일 비밀번호 찾기 본인 확인 이메일에서 받은 코드 검증하기)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkEmailVerificationForFindPassword(
        httpServletResponse: HttpServletResponse,
        verificationUid: Long,
        email: String,
        verificationCode: String
    ) {
        val emailVerification =
            db1RaillyLinkerCompanyTotalAuthFindPwWithEmailVerificationRepository.findByUidAndRowDeleteDateStr(
                verificationUid,
                "/"
            )

        if (emailVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (emailVerification.emailAddress != email) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(emailVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        val codeMatched = emailVerification.verificationSecret == verificationCode

        if (codeMatched) {
            // 코드 일치
            httpServletResponse.status = HttpStatus.OK.value()
        } else {
            // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
        }
    }


    // ----
    // (이메일 비밀번호 찾기 완료)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun findPasswordWithEmail(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.FindPasswordWithEmailInputVo
    ) {
        val emailVerification =
            db1RaillyLinkerCompanyTotalAuthFindPwWithEmailVerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (emailVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (emailVerification.emailAddress != inputVo.email) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(emailVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (emailVerification.verificationSecret == inputVo.verificationCode) { // 코드 일치
            // 입력 데이터 검증
            val memberEmail =
                db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findByEmailAddressAndRowDeleteDateStr(
                    inputVo.email,
                    "/"
                )

            if (memberEmail == null) {
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return
            }

            // 랜덤 비번 생성 후 세팅
            val newPassword = String.format("%09d", Random().nextInt(999999999)) // 랜덤 9자리 숫자
            memberEmail.totalAuthMember.accountPassword = passwordEncoder.encode(newPassword) // 비밀번호는 암호화
            db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberEmail.totalAuthMember)

            // 생성된 비번 이메일 전송
            emailSender.sendThymeLeafHtmlMail(
                "Springboot Mvc Project Template",
                arrayOf(inputVo.email),
                null,
                "Springboot Mvc Project Template 새 비밀번호 발급",
                "find_password_with_email/find_password_new_password_email",
                hashMapOf(
                    Pair("newPassword", newPassword)
                ),
                null,
                null,
                null,
                null
            )

            // 확인 완료된 검증 요청 정보 삭제
            emailVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthFindPwWithEmailVerificationRepository.save(emailVerification)

            // 모든 토큰 비활성화 처리
            // loginAccessToken 의 Iterable 가져오기
            val tokenInfoList =
                db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findAllByTotalAuthMemberAndLogoutDateAndRowDeleteDateStr(
                    memberEmail.totalAuthMember,
                    null,
                    "/"
                )

            // 발행되었던 모든 액세스 토큰 무효화 (다른 디바이스에선 사용중 로그아웃된 것과 동일한 효과)
            for (tokenInfo in tokenInfoList) {
                tokenInfo.logoutDate = LocalDateTime.now()
                db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenInfo)

                // 토큰 만료처리
                val tokenType = tokenInfo.tokenType
                val accessToken = tokenInfo.accessToken

                val accessTokenExpireRemainSeconds = when (tokenType) {
                    "Bearer" -> {
                        jwtTokenUtil.getRemainSeconds(accessToken)
                    }

                    else -> {
                        null
                    }
                }

                try {
                    redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                        "${tokenType}_${accessToken}",
                        Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                        accessTokenExpireRemainSeconds!! * 1000
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }
    }


    // ----
    // (전화번호 비밀번호 찾기 본인 인증 문자 발송)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun sendPhoneVerificationForFindPassword(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.SendPhoneVerificationForFindPasswordInputVo
    ): AuthController.SendPhoneVerificationForFindPasswordOutputVo? {
        // 입력 데이터 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByPhoneNumberAndRowDeleteDateStr(
                inputVo.phoneNumber,
                "/"
            )
        if (!memberExists) { // 회원 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 정보 저장 후 발송
        val verificationTimeSec: Long = 60 * 10
        val verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
        val memberFindPasswordPhoneNumberVerificationData =
            db1RaillyLinkerCompanyTotalAuthFindPwWithPhoneVerificationRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthFindPwWithPhoneVerification(
                    inputVo.phoneNumber,
                    verificationCode,
                    LocalDateTime.now().plusSeconds(verificationTimeSec)
                )
            )

        val phoneNumberSplit = inputVo.phoneNumber.split(")") // ["82", "010-0000-0000"]

        // 국가 코드 (ex : 82)
        val countryCode = phoneNumberSplit[0]

        // 전화번호 (ex : "01000000000")
        val phoneNumber = (phoneNumberSplit[1].replace("-", "")).replace(" ", "")

        val sendSmsResult = naverSmsSenderComponent.sendSms(
            NaverSmsSenderComponent.SendSmsInputVo(
                "SMS",
                countryCode,
                phoneNumber,
                "[Springboot Mvc Project Template - 비밀번호 찾기] 인증번호 [${verificationCode}]"
            )
        )

        if (!sendSmsResult) {
            throw Exception()
        }

        return AuthController.SendPhoneVerificationForFindPasswordOutputVo(
            memberFindPasswordPhoneNumberVerificationData.uid!!,
            memberFindPasswordPhoneNumberVerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        )
    }


    // ----
    // (전화번호 비밀번호 찾기 본인 확인 문자에서 받은 코드 검증하기)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkPhoneVerificationForFindPassword(
        httpServletResponse: HttpServletResponse,
        verificationUid: Long,
        phoneNumber: String,
        verificationCode: String
    ) {
        val phoneNumberVerification =
            db1RaillyLinkerCompanyTotalAuthFindPwWithPhoneVerificationRepository.findByUidAndRowDeleteDateStr(
                verificationUid,
                "/"
            )

        if (phoneNumberVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (phoneNumberVerification.phoneNumber != phoneNumber) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(phoneNumberVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        val codeMatched = phoneNumberVerification.verificationSecret == verificationCode

        if (codeMatched) {
            // 코드 일치
            httpServletResponse.status = HttpStatus.OK.value()
        } else {
            // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
        }
    }


    // ----
    // (전화번호 비밀번호 찾기 완료)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun findPasswordWithPhoneNumber(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.FindPasswordWithPhoneNumberInputVo
    ) {
        val phoneNumberVerification =
            db1RaillyLinkerCompanyTotalAuthFindPwWithPhoneVerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (phoneNumberVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (phoneNumberVerification.phoneNumber != inputVo.phoneNumber) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(phoneNumberVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (phoneNumberVerification.verificationSecret == inputVo.verificationCode) { // 코드 일치
            // 입력 데이터 검증
            val memberPhone =
                db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findByPhoneNumberAndRowDeleteDateStr(
                    inputVo.phoneNumber,
                    "/"
                )

            if (memberPhone == null) {
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return
            }

            // 랜덤 비번 생성 후 세팅
            val newPassword = String.format("%09d", Random().nextInt(999999999)) // 랜덤 9자리 숫자
            memberPhone.totalAuthMember.accountPassword = passwordEncoder.encode(newPassword) // 비밀번호는 암호화
            db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberPhone.totalAuthMember)

            val phoneNumberSplit = inputVo.phoneNumber.split(")") // ["82", "010-0000-0000"]

            // 국가 코드 (ex : 82)
            val countryCode = phoneNumberSplit[0]

            // 전화번호 (ex : "01000000000")
            val phoneNumber = (phoneNumberSplit[1].replace("-", "")).replace(" ", "")

            val sendSmsResult = naverSmsSenderComponent.sendSms(
                NaverSmsSenderComponent.SendSmsInputVo(
                    "SMS",
                    countryCode,
                    phoneNumber,
                    "[Springboot Mvc Project Template - 새 비밀번호] $newPassword"
                )
            )

            if (!sendSmsResult) {
                throw Exception()
            }

            // 확인 완료된 검증 요청 정보 삭제
            phoneNumberVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthFindPwWithPhoneVerificationRepository.save(
                phoneNumberVerification
            )

            // 모든 토큰 비활성화 처리
            // loginAccessToken 의 Iterable 가져오기
            val tokenInfoList =
                db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findAllByTotalAuthMemberAndLogoutDateAndRowDeleteDateStr(
                    memberPhone.totalAuthMember,
                    null,
                    "/"
                )

            // 발행되었던 모든 액세스 토큰 무효화 (다른 디바이스에선 사용중 로그아웃된 것과 동일한 효과)
            for (tokenInfo in tokenInfoList) {
                tokenInfo.logoutDate = LocalDateTime.now()
                db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenInfo)

                // 토큰 만료처리
                val tokenType = tokenInfo.tokenType
                val accessToken = tokenInfo.accessToken

                val accessTokenExpireRemainSeconds = when (tokenType) {
                    "Bearer" -> {
                        jwtTokenUtil.getRemainSeconds(accessToken)
                    }

                    else -> {
                        null
                    }
                }

                try {
                    redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                        "${tokenType}_${accessToken}",
                        Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                        accessTokenExpireRemainSeconds!! * 1000
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }
    }


    // ----
    // (내 이메일 리스트 가져오기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getMyEmailList(
        httpServletResponse: HttpServletResponse,
        authorization: String
    ): AuthController.GetMyEmailListOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val emailEntityList =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )
        val emailList = ArrayList<AuthController.GetMyEmailListOutputVo.EmailInfo>()
        for (emailEntity in emailEntityList) {
            emailList.add(
                AuthController.GetMyEmailListOutputVo.EmailInfo(
                    emailEntity.uid!!,
                    emailEntity.emailAddress,
                    emailEntity.priority
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.GetMyEmailListOutputVo(
            emailList
        )
    }


    // ----
    // (내 전화번호 리스트 가져오기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getMyPhoneNumberList(
        httpServletResponse: HttpServletResponse,
        authorization: String
    ): AuthController.GetMyPhoneNumberListOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val phoneEntityList =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )
        val phoneNumberList = ArrayList<AuthController.GetMyPhoneNumberListOutputVo.PhoneInfo>()
        for (phoneEntity in phoneEntityList) {
            phoneNumberList.add(
                AuthController.GetMyPhoneNumberListOutputVo.PhoneInfo(
                    phoneEntity.uid!!,
                    phoneEntity.phoneNumber,
                    phoneEntity.priority
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.GetMyPhoneNumberListOutputVo(
            phoneNumberList
        )
    }


    // ----
    // (내 OAuth2 로그인 리스트 가져오기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getMyOauth2List(
        httpServletResponse: HttpServletResponse,
        authorization: String
    ): AuthController.GetMyOauth2ListOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val oAuth2EntityList =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )
        val myOAuth2List = ArrayList<AuthController.GetMyOauth2ListOutputVo.OAuth2Info>()
        for (oAuth2Entity in oAuth2EntityList) {
            myOAuth2List.add(
                AuthController.GetMyOauth2ListOutputVo.OAuth2Info(
                    oAuth2Entity.uid!!,
                    oAuth2Entity.oauth2TypeCode.toInt(),
                    oAuth2Entity.oauth2Id
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.GetMyOauth2ListOutputVo(
            myOAuth2List
        )
    }


    // ----
    // (이메일 추가하기 본인 인증 이메일 발송 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun sendEmailVerificationForAddNewEmail(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.SendEmailVerificationForAddNewEmailInputVo,
        authorization: String
    ): AuthController.SendEmailVerificationForAddNewEmailOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 입력 데이터 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByEmailAddressAndRowDeleteDateStr(
                inputVo.email,
                "/"
            )

        if (memberExists) { // 기존 회원 존재
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 정보 저장 후 이메일 발송
        val verificationTimeSec: Long = 60 * 10
        val verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
        val memberRegisterEmailVerificationData = db1RaillyLinkerCompanyTotalAuthAddEmailVerificationRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthAddEmailVerification(
                memberData,
                inputVo.email,
                verificationCode,
                LocalDateTime.now().plusSeconds(verificationTimeSec)
            )
        )

        emailSender.sendThymeLeafHtmlMail(
            "Springboot Mvc Project Template",
            arrayOf(inputVo.email),
            null,
            "Springboot Mvc Project Template 이메일 추가 - 본인 계정 확인용 이메일입니다.",
            "send_email_verification_for_add_new_email/add_email_verification_email",
            hashMapOf(
                Pair("verificationCode", verificationCode)
            ),
            null,
            null,
            null,
            null
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.SendEmailVerificationForAddNewEmailOutputVo(
            memberRegisterEmailVerificationData.uid!!,
            memberRegisterEmailVerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        )
    }


    // ----
    // (이메일 추가하기 본인 확인 이메일에서 받은 코드 검증하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkEmailVerificationForAddNewEmail(
        httpServletResponse: HttpServletResponse,
        verificationUid: Long,
        email: String,
        verificationCode: String,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val emailVerification =
            db1RaillyLinkerCompanyTotalAuthAddEmailVerificationRepository.findByUidAndRowDeleteDateStr(
                verificationUid,
                "/"
            )

        if (emailVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (emailVerification.totalAuthMember.uid!! != memberUid ||
            emailVerification.emailAddress != email
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(emailVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        val codeMatched = emailVerification.verificationSecret == verificationCode

        if (codeMatched) {
            // 코드 일치
            httpServletResponse.status = HttpStatus.OK.value()
        } else {
            // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
        }
    }


    // ----
    // (이메일 추가하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun addNewEmail(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.AddNewEmailInputVo,
        authorization: String
    ): AuthController.AddNewEmailOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val emailVerification =
            db1RaillyLinkerCompanyTotalAuthAddEmailVerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (emailVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (emailVerification.totalAuthMember.uid!! != memberUid ||
            emailVerification.emailAddress != inputVo.email
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (LocalDateTime.now().isAfter(emailVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (emailVerification.verificationSecret == inputVo.verificationCode) { // 코드 일치
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByEmailAddressAndRowDeleteDateStr(
                    inputVo.email,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return null
            }

            // 이메일 추가
            val memberEmailData = db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberEmail(
                    memberData,
                    inputVo.email,
                    if (inputVo.priority == null) {
                        // null 설정이라면 현재 가장 큰 priority 적용
                        val emailEntityList =
                            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                                memberData,
                                "/"
                            )
                        if (emailEntityList.isEmpty()) {
                            0
                        } else {
                            emailEntityList.first().priority
                        }
                    } else {
                        inputVo.priority
                    }
                )
            )

            // 확인 완료된 검증 요청 정보 삭제
            emailVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthAddEmailVerificationRepository.save(emailVerification)

            httpServletResponse.status = HttpStatus.OK.value()
            return AuthController.AddNewEmailOutputVo(
                memberEmailData.uid!!
            )
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }
    }


    // ----
    // (내 이메일 제거하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteMyEmail(
        httpServletResponse: HttpServletResponse,
        emailUid: Long,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 내 계정에 등록된 모든 이메일 리스트 가져오기
        val myEmailList =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )

        if (myEmailList.isEmpty()) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        var myEmailVo: Db1_RaillyLinkerCompany_TotalAuthMemberEmail? = null

        for (myEmail in myEmailList) {
            if (myEmail.uid == emailUid) {
                myEmailVo = myEmail
                break
            }
        }

        if (myEmailVo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        val isOauth2Exists =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        val isMemberPhoneExists =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        if (isOauth2Exists ||
            (memberData.accountPassword != null && myEmailList.size > 1) ||
            (memberData.accountPassword != null && isMemberPhoneExists)
        ) {
            // 이메일 지우기
            myEmailVo.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.save(myEmailVo)

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else {
            // 이외에 사용 가능한 로그인 정보가 존재하지 않을 때
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }
    }


    // ----
    // (이메일 가중치 수정 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchEmailPriority(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.PatchEmailPriorityInputVo,
        emailUid: Long,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val emailEntity =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findByTotalAuthMemberAndUidAndRowDeleteDateStr(
                memberData,
                emailUid,
                "/"
            )

        if (emailEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        emailEntity.priority =
            if (inputVo.priority == null) {
                // null 설정이라면 현재 가장 큰 priority 적용
                val emailEntityList =
                    db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                        memberData,
                        "/"
                    )
                if (emailEntityList.isEmpty()) {
                    0
                } else {
                    emailEntityList.first().priority
                }
            } else {
                inputVo.priority
            }

        db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.save(emailEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (전화번호 추가하기 본인 인증 문자 발송 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun sendPhoneVerificationForAddNewPhoneNumber(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.SendPhoneVerificationForAddNewPhoneNumberInputVo,
        authorization: String
    ): AuthController.SendPhoneVerificationForAddNewPhoneNumberOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 입력 데이터 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByPhoneNumberAndRowDeleteDateStr(
                inputVo.phoneNumber,
                "/"
            )

        if (memberExists) { // 기존 회원 존재
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 정보 저장 후 이메일 발송
        val verificationTimeSec: Long = 60 * 10
        val verificationCode = String.format("%06d", Random().nextInt(999999)) // 랜덤 6자리 숫자
        val memberAddPhoneNumberVerificationData =
            db1RaillyLinkerCompanyTotalAuthAddPhoneVerificationRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthAddPhoneVerification(
                    memberData,
                    inputVo.phoneNumber,
                    verificationCode,
                    LocalDateTime.now().plusSeconds(verificationTimeSec)
                )
            )

        val phoneNumberSplit = inputVo.phoneNumber.split(")") // ["82", "010-0000-0000"]

        // 국가 코드 (ex : 82)
        val countryCode = phoneNumberSplit[0]

        // 전화번호 (ex : "01000000000")
        val phoneNumber = (phoneNumberSplit[1].replace("-", "")).replace(" ", "")

        val sendSmsResult = naverSmsSenderComponent.sendSms(
            NaverSmsSenderComponent.SendSmsInputVo(
                "SMS",
                countryCode,
                phoneNumber,
                "[Springboot Mvc Project Template - 전화번호 추가] 인증번호 [${verificationCode}]"
            )
        )

        if (!sendSmsResult) {
            throw Exception()
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.SendPhoneVerificationForAddNewPhoneNumberOutputVo(
            memberAddPhoneNumberVerificationData.uid!!,
            memberAddPhoneNumberVerificationData.verificationExpireWhen.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        )
    }


    // ----
    // (전화번호 추가하기 본인 확인 문자에서 받은 코드 검증하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun checkPhoneVerificationForAddNewPhoneNumber(
        httpServletResponse: HttpServletResponse,
        verificationUid: Long,
        phoneNumber: String,
        verificationCode: String,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )

        val phoneNumberVerification =
            db1RaillyLinkerCompanyTotalAuthAddPhoneVerificationRepository.findByUidAndRowDeleteDateStr(
                verificationUid,
                "/"
            )

        if (phoneNumberVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (phoneNumberVerification.totalAuthMember.uid!! != memberUid ||
            phoneNumberVerification.phoneNumber != phoneNumber
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (LocalDateTime.now().isAfter(phoneNumberVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // 입력 코드와 발급된 코드와의 매칭
        if (phoneNumberVerification.verificationSecret == verificationCode) {
            // 코드 일치
            httpServletResponse.status = HttpStatus.OK.value()
        } else {
            // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
        }
    }


    // ----
    // (전화번호 추가하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun addNewPhoneNumber(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.AddNewPhoneNumberInputVo,
        authorization: String
    ): AuthController.AddNewPhoneNumberOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val phoneNumberVerification =
            db1RaillyLinkerCompanyTotalAuthAddPhoneVerificationRepository.findByUidAndRowDeleteDateStr(
                inputVo.verificationUid,
                "/"
            )

        if (phoneNumberVerification == null) { // 해당 이메일 검증을 요청한적이 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (phoneNumberVerification.totalAuthMember.uid!! != memberUid ||
            phoneNumberVerification.phoneNumber != inputVo.phoneNumber
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (LocalDateTime.now().isAfter(phoneNumberVerification.verificationExpireWhen)) {
            // 만료됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 입력 코드와 발급된 코드와의 매칭
        val codeMatched = phoneNumberVerification.verificationSecret == inputVo.verificationCode

        if (codeMatched) { // 코드 일치
            val isUserExists =
                db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByPhoneNumberAndRowDeleteDateStr(
                    inputVo.phoneNumber,
                    "/"
                )
            if (isUserExists) { // 기존 회원이 있을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "4")
                return null
            }

            // 추가
            val memberPhoneData = db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.save(
                Db1_RaillyLinkerCompany_TotalAuthMemberPhone(
                    memberData,
                    inputVo.phoneNumber,
                    if (inputVo.priority == null) {
                        // null 설정이라면 현재 가장 큰 priority 적용
                        val phoneEntityList =
                            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                                memberData,
                                "/"
                            )
                        if (phoneEntityList.isEmpty()) {
                            0
                        } else {
                            phoneEntityList.first().priority
                        }
                    } else {
                        inputVo.priority
                    }
                )
            )

            // 확인 완료된 검증 요청 정보 삭제
            phoneNumberVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthAddPhoneVerificationRepository.save(phoneNumberVerification)

            httpServletResponse.status = HttpStatus.OK.value()
            return AuthController.AddNewPhoneNumberOutputVo(
                memberPhoneData.uid!!
            )
        } else { // 코드 불일치
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }
    }


    // ----
    // (내 전화번호 제거하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteMyPhoneNumber(
        httpServletResponse: HttpServletResponse,
        phoneUid: Long,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 내 계정에 등록된 모든 전화번호 리스트 가져오기
        val myPhoneList =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )

        if (myPhoneList.isEmpty()) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        var myPhoneVo: Db1_RaillyLinkerCompany_TotalAuthMemberPhone? = null

        for (myPhone in myPhoneList) {
            if (myPhone.uid == phoneUid) {
                myPhoneVo = myPhone
                break
            }
        }

        if (myPhoneVo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        val isOauth2Exists =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        val isMemberEmailExists =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        if (isOauth2Exists ||
            (memberData.accountPassword != null && myPhoneList.size > 1) ||
            (memberData.accountPassword != null && isMemberEmailExists)
        ) {
            // 전화번호 지우기
            myPhoneVo.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.save(myPhoneVo)

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else {
            // 이외에 사용 가능한 로그인 정보가 존재하지 않을 때
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }
    }


    // ----
    // (전화번호 가중치 수정 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchPhoneNumberPriority(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.PatchPhoneNumberPriorityInputVo,
        phoneUid: Long,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val phoneEntity =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findByTotalAuthMemberAndUidAndRowDeleteDateStr(
                memberData,
                phoneUid,
                "/"
            )

        if (phoneEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        phoneEntity.priority =
            if (inputVo.priority == null) {
                // null 설정이라면 현재 가장 큰 priority 적용
                val phoneEntityList =
                    db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                        memberData,
                        "/"
                    )
                if (phoneEntityList.isEmpty()) {
                    0
                } else {
                    phoneEntityList.first().priority
                }
            } else {
                inputVo.priority
            }

        db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.save(phoneEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (OAuth2 추가하기 (Access Token) <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun addNewOauth2WithAccessToken(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.AddNewOauth2WithAccessTokenInputVo,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val snsTypeCode: Short
        val snsId: String

        // (정보 검증 로직 수행)
        when (inputVo.oauth2TypeCode) {
            1 -> { // GOOGLE
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.wwwGoogleapisComRequestApi.getOauth2V1UserInfo(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return
                }

                snsTypeCode = 1
                snsId = response.body()!!.id!!
            }

            2 -> { // NAVER
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.openapiNaverComRequestApi.getV1NidMe(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return
                }

                snsTypeCode = 2
                snsId = response.body()!!.response.id
            }

            3 -> { // KAKAO TALK
                // 클라이언트에서 받은 access 토큰으로 멤버 정보 요청
                val response = networkRetrofit2.kapiKakaoComRequestApi.getV2UserMe(
                    inputVo.oauth2AccessToken
                ).execute()

                // 액세트 토큰 정상 동작 확인
                if (response.code() != 200 ||
                    response.body() == null
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return
                }

                snsTypeCode = 3
                snsId = response.body()!!.id.toString()
            }

            else -> {
                classLogger.info("SNS Login Type ${inputVo.oauth2TypeCode} Not Supported")
                httpServletResponse.status = 400
                return
            }
        }

        // 사용중인지 아닌지 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                snsTypeCode,
                snsId,
                "/"
            )

        if (memberExists) { // 이미 사용중인 SNS 인증
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // SNS 인증 추가
        db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login(
                memberData,
                snsTypeCode,
                snsId
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (OAuth2 추가하기 (Id Token) <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun addNewOauth2WithIdToken(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.AddNewOauth2WithIdTokenInputVo,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val snsTypeCode: Short
        val snsId: String

        // (정보 검증 로직 수행)
        when (inputVo.oauth2TypeCode.toInt()) {
            4 -> { // Apple
                val appleInfo = appleOAuthHelperUtil.getAppleMemberData(inputVo.oauth2IdToken)

                if (appleInfo != null) {
                    snsId = appleInfo.snsId
                } else {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return
                }

                snsTypeCode = 4
            }

            else -> {
                classLogger.info("SNS Login Type ${inputVo.oauth2TypeCode} Not Supported")
                httpServletResponse.status = 400
                return
            }
        }

        // 사용중인지 아닌지 검증
        val memberExists =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
                snsTypeCode,
                snsId,
                "/"
            )

        if (memberExists) { // 이미 사용중인 SNS 인증
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // SNS 인증 추가
        db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login(
                memberData,
                snsTypeCode,
                snsId
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (내 OAuth2 제거하기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteMyOauth2(
        httpServletResponse: HttpServletResponse,
        oAuth2Uid: Long,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 내 계정에 등록된 모든 인증 리스트 가져오기
        val myOAuth2List =
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        if (myOAuth2List.isEmpty()) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        var myOAuth2Vo: Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login? = null

        for (myOAuth2 in myOAuth2List) {
            if (myOAuth2.uid == oAuth2Uid) {
                myOAuth2Vo = myOAuth2
                break
            }
        }

        if (myOAuth2Vo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        val isMemberEmailExists =
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.existsByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        val isMemberPhoneExists =
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.existsByTotalAuthMemberAndRowDeleteDateStr(
                memberData,
                "/"
            )

        if (myOAuth2List.size > 1 ||
            (memberData.accountPassword != null && isMemberEmailExists) ||
            (memberData.accountPassword != null && isMemberPhoneExists)
        ) {
            // 로그인 정보 지우기
            myOAuth2Vo.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.save(myOAuth2Vo)

            httpServletResponse.status = HttpStatus.OK.value()
            return
        } else {
            // 이외에 사용 가능한 로그인 정보가 존재하지 않을 때
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }
    }


    // ----
    // (회원탈퇴 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun withdrawalMembership(
        httpServletResponse: HttpServletResponse,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // member_phone, member_email, member_role, member_sns_oauth2, member_profile, loginAccessToken 비활성화

        // !!!회원과 관계된 처리!!
        // cascade 설정이 되어있으므로 memberData 를 참조중인 테이블은 자동으로 삭제됩니다. 파일같은 경우에는 수동으로 처리하세요.
//        val profileData = memberProfileDataRepository.findAllByMemberData(memberData)
//        for (profile in profileData) {
//            // !!!프로필 이미지 파일 삭제하세요!!!
//        }

        for (totalAuthMemberRole in memberData.totalAuthMemberRoleList) {
            totalAuthMemberRole.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.save(totalAuthMemberRole)
        }

        for (totalAuthMemberEmail in memberData.totalAuthMemberEmailList) {
            totalAuthMemberEmail.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberEmailRepository.save(totalAuthMemberEmail)
        }

        for (totalAuthMemberPhone in memberData.totalAuthMemberPhoneList) {
            totalAuthMemberPhone.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository.save(totalAuthMemberPhone)
        }

        for (totalAuthMemberProfile in memberData.totalAuthMemberProfileList) {
            totalAuthMemberProfile.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(totalAuthMemberProfile)
        }

        for (totalAuthAddEmailVerification in memberData.totalAuthAddEmailVerificationList) {
            totalAuthAddEmailVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthAddEmailVerificationRepository.save(totalAuthAddEmailVerification)
        }

        for (totalAuthAddPhoneVerification in memberData.totalAuthAddPhoneVerificationList) {
            totalAuthAddPhoneVerification.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthAddPhoneVerificationRepository.save(
                totalAuthAddPhoneVerification
            )
        }


        for (totalAuthMemberLockHistory in memberData.totalAuthMemberLockHistoryList) {
            totalAuthMemberLockHistory.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberLockHistoryRepository.save(totalAuthMemberLockHistory)
        }

        for (totalAuthMemberOauth2Login in memberData.totalAuthMemberOauth2LoginList) {
            totalAuthMemberOauth2Login.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthMemberOauth2LoginRepository.save(totalAuthMemberOauth2Login)
        }

        // 이미 발행된 토큰 만료처리
        val tokenEntityList =
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.findAllByTotalAuthMemberAndAccessTokenExpireWhenAfterAndRowDeleteDateStr(
                memberData,
                LocalDateTime.now(),
                "/"
            )
        for (tokenEntity in tokenEntityList) {
            tokenEntity.logoutDate = LocalDateTime.now()
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(tokenEntity)

            val tokenType = tokenEntity.tokenType
            val accessToken = tokenEntity.accessToken

            val accessTokenExpireRemainSeconds = when (tokenType) {
                "Bearer" -> {
                    jwtTokenUtil.getRemainSeconds(accessToken)
                }

                else -> {
                    null
                }
            }

            try {
                redis1MapTotalAuthForceExpireAuthorizationSet.saveKeyValue(
                    "${tokenType}_${accessToken}",
                    Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo(),
                    accessTokenExpireRemainSeconds!! * 1000
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (totalAuthLogInTokenHistory in memberData.totalAuthLogInTokenHistoryList) {
            totalAuthLogInTokenHistory.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyTotalAuthLogInTokenHistoryRepository.save(totalAuthLogInTokenHistory)
        }

        // 회원탈퇴 처리
        memberData.rowDeleteDateStr =
            LocalDateTime.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        db1RaillyLinkerCompanyTotalAuthMemberRepository.save(memberData)

        // kafkaProducer1 에 토픽 메세지 발행
        kafka1MainProducer.sendMessageFromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMember(
            Kafka1MainProducer.SendMessageFromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberInputVo(
                memberUid
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (내 Profile 이미지 정보 리스트 가져오기 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getMyProfileList(
        httpServletResponse: HttpServletResponse,
        authorization: String
    ): AuthController.GetMyProfileListOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val profileData =
            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                memberData,
                "/"
            )

        val myProfileList: ArrayList<AuthController.GetMyProfileListOutputVo.ProfileInfo> =
            ArrayList()
        for (profile in profileData) {
            myProfileList.add(
                AuthController.GetMyProfileListOutputVo.ProfileInfo(
                    profile.uid!!,
                    profile.imageFullUrl,
                    profile.priority
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.GetMyProfileListOutputVo(
            myProfileList
        )
    }


    // ----
    // (내 프로필 삭제 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteMyProfile(
        authorization: String,
        httpServletResponse: HttpServletResponse,
        profileUid: Long
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 프로필 가져오기
        val profileData =
            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
                profileUid,
                memberData,
                "/"
            )

        if (profileData == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        // 프로필 비활성화
        profileData.rowDeleteDateStr =
            LocalDateTime.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(profileData)
        // !!!프로필 이미지 파일 삭제하세요!!!

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (내 프로필 가중치 수정 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchProfilePriority(
        httpServletResponse: HttpServletResponse,
        inputVo: AuthController.PatchProfilePriorityInputVo,
        profileUid: Long,
        authorization: String
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val profileEntity =
            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
                profileUid,
                memberData,
                "/"
            )

        if (profileEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        profileEntity.priority =
            if (inputVo.priority == null) {
                // null 설정이라면 현재 가장 큰 priority 적용
                val profileEntityList =
                    db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                        memberData,
                        "/"
                    )
                if (profileEntityList.isEmpty()) {
                    0
                } else {
                    profileEntityList.first().priority
                }
            } else {
                inputVo.priority
            }

        db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(profileEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (내 프로필 이미지 추가 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun addNewProfile(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        inputVo: AuthController.AddNewProfileInputVo
    ): AuthController.AddNewProfileOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        // 저장된 프로필 이미지 파일을 다운로드 할 수 있는 URL
        val savedProfileImageUrl: String

        // 프로필 이미지 파일 저장

        //----------------------------------------------------------------------------------------------------------
        // 프로필 이미지를 서버 스토리지에 저장할 때 사용하는 방식
        // 파일 저장 기본 디렉토리 경로
        val saveDirectoryPath: Path = Paths.get("./by_product_files/auth/member/profile").toAbsolutePath().normalize()

        val savedFileName = customUtil.multipartFileLocalSave(
            saveDirectoryPath,
            null,
            inputVo.profileImageFile
        )

        savedProfileImageUrl = "${externalAccessAddress}/auth/member-profile/$savedFileName"
        //----------------------------------------------------------------------------------------------------------

        val profileData = db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.save(
            Db1_RaillyLinkerCompany_TotalAuthMemberProfile(
                memberData,
                savedProfileImageUrl,
                if (inputVo.priority == null) {
                    // null 설정이라면 현재 가장 큰 priority 적용
                    val profileEntityList =
                        db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                            memberData,
                            "/"
                        )
                    if (profileEntityList.isEmpty()) {
                        0
                    } else {
                        profileEntityList.first().priority
                    }
                } else {
                    inputVo.priority
                }
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.AddNewProfileOutputVo(
            profileData.uid!!,
            profileData.imageFullUrl
        )
    }


    // ----
    // (by_product_files/member/profile 폴더에서 파일 다운받기)
    fun downloadProfileFile(
        httpServletResponse: HttpServletResponse,
        fileName: String
    ): ResponseEntity<Resource>? {
        // 프로젝트 루트 경로 (프로젝트 settings.gradle 이 있는 경로)
        val projectRootAbsolutePathString: String = File("").absolutePath

        // 파일 절대 경로 및 파일명
        val serverFilePathObject =
            Paths.get("$projectRootAbsolutePathString/by_product_files/auth/member/profile/$fileName")

        when {
            Files.isDirectory(serverFilePathObject) -> {
                // 파일이 디렉토리일때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "1")
                return null
            }

            Files.notExists(serverFilePathObject) -> {
                // 파일이 없을 때
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "1")
                return null
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return ResponseEntity<Resource>(
            InputStreamResource(Files.newInputStream(serverFilePathObject)),
            HttpHeaders().apply {
                this.contentDisposition = ContentDisposition.builder("attachment")
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build()
                this.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(serverFilePathObject))
            },
            HttpStatus.OK
        )
    }


    // ----
    // (Redis Key-Value 모두 조회 테스트)
    fun selectAllRedisKeyValueSample(httpServletResponse: HttpServletResponse): AuthController.SelectAllRedisKeyValueSampleOutputVo? {
        // 전체 조회 테스트
        val keyValueList = redis1MapTotalAuthForceExpireAuthorizationSet.findAllKeyValues()

        val testEntityListVoList =
            ArrayList<AuthController.SelectAllRedisKeyValueSampleOutputVo.KeyValueVo>()
        for (keyValue in keyValueList) {
            testEntityListVoList.add(
                AuthController.SelectAllRedisKeyValueSampleOutputVo.KeyValueVo(
                    keyValue.key,
                    keyValue.expireTimeMs
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return AuthController.SelectAllRedisKeyValueSampleOutputVo(
            testEntityListVoList
        )
    }
}