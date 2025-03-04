package com.raillylinker.configurations

import com.raillylinker.redis_map_components.redis1_main.Redis1_Map_TotalAuthForceExpireAuthorizationSet
import com.raillylinker.util_components.JwtTokenUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter

// [서비스 보안 시큐리티 설정]
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (비밀번호 인코딩, 매칭시 사용할 객체)
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        return UrlBasedCorsConfigurationSource().apply {
            this.registerCorsConfiguration(
                "/**", // 아래 설정을 적용할 컨트롤러 패턴
                CorsConfiguration().apply {
                    allowedOriginPatterns = listOf("*") // 허가 클라이언트 주소
                    allowedMethods = listOf("*") // 허가할 클라이언트 리퀘스트 http method
                    allowedHeaders = listOf("*") // 허가할 클라이언트 발신 header
                    exposedHeaders = listOf("*") // 허가할 클라이언트 수신 header
                    maxAge = 3600L
                    allowCredentials = true
                }
            )
        }
    }

    @Bean
    protected fun sessionRegistry(): SessionRegistryImpl {
        return SessionRegistryImpl()
    }

    // !!!경로별 적용할 Security 필터 체인 Bean 작성하기!!!

    // [기본적으로 모든 요청 Open]
    @Bean
    @Order(Int.MAX_VALUE)
    fun securityFilterChainMainSc(
        http: HttpSecurity
    ): SecurityFilterChain {
        // cors 적용(서로 다른 origin 의 웹화면에서 리퀘스트 금지)
        http.cors {}

        // (사이즈간 위조 요청(Cross site Request forgery) 방지 설정)
        // csrf 설정시 POST, PUT, DELETE 요청으로부터 보호하며 csrf 토큰이 포함되어야 요청을 받아들이게 됨
        // Rest API 에선 Token 이 요청의 위조 방지 역할을 하기에 비활성화
        http.csrf { csrfCustomizer ->
            csrfCustomizer.disable()
        }

        http.httpBasic { httpBasicCustomizer ->
            httpBasicCustomizer.disable()
        }

        // Token 인증을 위한 세션 비활성화
        http.sessionManagement { sessionManagementCustomizer ->
            sessionManagementCustomizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

        // 스프링 시큐리티 기본 로그인 화면 비활성화
        http.formLogin { formLoginCustomizer ->
            formLoginCustomizer.disable()
        }

        // 스프링 시큐리티 기본 로그아웃 비활성화
        http.logout { logoutCustomizer ->
            logoutCustomizer.disable()
        }

        // (API 요청 제한)
        // 기본적으로 모두 Open
        http.authorizeHttpRequests { authorizeHttpRequestsCustomizer ->
            // 모든 요청 허용
            authorizeHttpRequestsCustomizer.anyRequest().permitAll()
        }

        return http.build()
    }


    // ----
    // [JWT 토큰 인증 체계 적용 필터 체인]
    @Bean
    @Order(1)
    fun securityFilterChainToken(
        http: HttpSecurity,
        authTokenFilterTotalAuth: AuthTokenFilterTotalAuth
    ): SecurityFilterChain {
        val securityMatcher = http.securityMatcher(*authTokenFilterTotalAuth.securityUrlList.toTypedArray())

        securityMatcher.headers { headersCustomizer ->
            // iframe 허용 설정
            // 기본은 허용하지 않음, sameOrigin 은 같은 origin 일 때에만 허용하고, disable 은 모두 허용
            headersCustomizer.frameOptions { frameOptionsConfig ->
                frameOptionsConfig.sameOrigin()
            }
        }

        // cors 적용(서로 다른 origin 의 웹화면에서 리퀘스트 금지)
        securityMatcher.cors {}

        // (사이즈간 위조 요청(Cross site Request forgery) 방지 설정)
        // csrf 설정시 POST, PUT, DELETE 요청으로부터 보호하며 csrf 토큰이 포함되어야 요청을 받아들이게 됨
        // Rest API 에선 Token 이 요청의 위조 방지 역할을 하기에 비활성화
        securityMatcher.csrf { csrfCustomizer ->
            csrfCustomizer.disable()
        }

        securityMatcher.httpBasic { httpBasicCustomizer ->
            httpBasicCustomizer.disable()
        }

        // Token 인증을 위한 세션 비활성화
        securityMatcher.sessionManagement { sessionManagementCustomizer ->
            sessionManagementCustomizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

        // (Token 인증 검증 필터 연결)
        // API 요청마다 헤더로 들어오는 인증 토큰 유효성을 검증
        securityMatcher.addFilterBefore(
            authTokenFilterTotalAuth,
            UsernamePasswordAuthenticationFilter::class.java
        )

        // 스프링 시큐리티 기본 로그인 화면 비활성화
        securityMatcher.formLogin { formLoginCustomizer ->
            formLoginCustomizer.disable()
        }

        // 스프링 시큐리티 기본 로그아웃 비활성화
        securityMatcher.logout { logoutCustomizer ->
            logoutCustomizer.disable()
        }

        // 예외처리
        securityMatcher.exceptionHandling { exceptionHandlingCustomizer ->
            // 비인증(Security Context 에 멤버 정보가 없음) 처리
            exceptionHandlingCustomizer.authenticationEntryPoint { _, response, _ -> // Http Status 401
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: UnAuthorized")
            }
            // 비인가(멤버 권한이 충족되지 않음) 처리
            exceptionHandlingCustomizer.accessDeniedHandler { _, response, _ -> // Http Status 403
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Error: Forbidden")
            }
        }

        // (API 요청 제한)
        // 기본적으로 모두 Open
        securityMatcher.authorizeHttpRequests { authorizeHttpRequestsCustomizer ->
            authorizeHttpRequestsCustomizer.anyRequest().permitAll()
            /*
                본 서버 접근 보안은 블랙 리스트 방식을 사용합니다.
                일반적으로 모든 요청을 허용하며, 인증/인가가 필요한 부분에는,
                @PreAuthorize("isAuthenticated() and (hasRole('ROLE_DEVELOPER') or hasRole('ROLE_ADMIN'))")
                위와 같은 어노테이션을 접근 통제하고자 하는 API 위에 달아주면 인증 필터가 동작하게 됩니다.
             */
        }

        return securityMatcher.build()
    }

    // 인증 토큰 검증 필터 - API 요청마다 검증 실행
    @Component
    class AuthTokenFilterTotalAuth(
        private val expireTokenRedis: Redis1_Map_TotalAuthForceExpireAuthorizationSet,
        private val jwtTokenUtil: JwtTokenUtil
    ) : OncePerRequestFilter() {
        // <멤버 변수 공간>
        // !!!아래 인증 관련 설정 정보 변수들의 값을 수정하기!!!
        // 계정 설정 - JWT 비밀키
        val authJwtSecretKeyString: String = "123456789abcdefghijklmnopqrstuvw"

        // 계정 설정 - JWT AccessToken 유효기간(초)
        val authJwtAccessTokenExpirationTimeSec: Long = 60L * 30L // 30분

        // 계정 설정 - JWT RefreshToken 유효기간(초)
        val authJwtRefreshTokenExpirationTimeSec: Long = 60L * 60L * 24L * 7L // 7일

        // 계정 설정 - JWT 본문 암호화 AES256 IV 16자
        val authJwtClaimsAes256InitializationVector: String = "odkejduc726dj48d"

        // 계정 설정 - JWT 본문 암호화 AES256 암호키 32자
        val authJwtClaimsAes256EncryptionKey: String = "8fu3jd0ciiu3384hfucy36dye9sjv7b3"

        // 계정 설정 - JWT 발행자
        val authJwtIssuer: String = "com.raillylinker.my-service"

        // 본 시큐리티 필터가 관리할 주소 체계
        val securityUrlList = listOf(
            "/auth/**"
        ) // 위 모든 경로에 적용

        // ---------------------------------------------------------------------------------------------
        // <공개 메소드 공간>
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            // 패턴에 매치되는지 확인
            var patternMatch = false

            for (filterPattern in securityUrlList) {
                if (AntPathRequestMatcher(filterPattern).matches(request)) {
                    patternMatch = true
                    break
                }
            }

            if (!patternMatch) {
                // 이 필터를 실행해야 할 패턴이 아님.

                // 다음 필터 실행
                filterChain.doFilter(request, response)
                return
            }

            // 인증 결과 == 유저 권한 리스트
            val authResult = checkRequestAuthorization(request)

            // 인증 결과가 null 입니다.
            if (authResult == null) {
                // 다음 필터 실행
                filterChain.doFilter(request, response)
                return
            }

            // (검증된 멤버 정보와 권한 정보를 Security Context 에 입력)
            // authentication 정보가 context 에 존재하는지 여부로 로그인 여부를 확인
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                    null, // 세션을 유지하지 않으니 굳이 입력할 필요가 없음
                    null, // 세션을 유지하지 않으니 굳이 입력할 필요가 없음
                    authResult // 멤버 권한 리스트만 입력해주어 권한 확인에 사용
                ).apply {
                    this.details =
                        WebAuthenticationDetailsSource().buildDetails(request)
                }

            filterChain.doFilter(request, response)
            return
        }

        // (request 의 인증/인가 정보 처리)
        // HttpServletRequest 에서 인증/인가 정보를 추출해 검증합니다.
        // 정상 인증시 권한 리스트가 반환되며, 인증 실패시 null 이 반환됩니다.
        fun checkRequestAuthorization(request: HttpServletRequest): ArrayList<GrantedAuthority>? {
            // (리퀘스트에서 가져온 AccessToken 검증)
            // 헤더의 Authorization 의 값 가져오기
            // 정상적인 토큰값은 "Bearer {Token String}" 형식으로 온다고 가정.
            val authorization = request.getHeader("Authorization")
                ?: // Authorization 에 토큰을 넣지 않은 경우 = 인증 / 인가를 받을 의도가 없음
                return null // ex : "Bearer aqwer1234"

            // 타입과 토큰을 분리
            val authorizationSplit = authorization.split(" ") // ex : ["Bearer", "qwer1234"]
            if (authorizationSplit.size < 2) {
                return null
            }

            // 타입으로 추정되는 문장이 존재할 때
            // 타입 분리
            val tokenType = authorizationSplit[0].trim() // 첫번째 단어는 토큰 타입
            val accessToken = authorizationSplit[1].trim() // 앞의 타입을 자르고 남은 토큰

            // 강제 토큰 만료 검증
            val forceExpired = try {
                expireTokenRedis.findKeyValue(tokenType + "_" + accessToken)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } != null

            if (forceExpired) {
                return null
            }

            // 토큰 검증
            if (accessToken == "") {
                return null
            }

            when (tokenType.lowercase()) { // 타입 검증
                "bearer" -> { // Bearer JWT 토큰 검증
                    // 토큰 문자열 해석 가능여부 확인
                    val accessTokenType: String? = try {
                        jwtTokenUtil.getTokenType(accessToken)
                    } catch (_: Exception) {
                        null
                    }

                    if (accessTokenType == null || // 해석 불가능한 JWT 토큰
                        accessTokenType.lowercase() != "jwt" || // 토큰 타입이 JWT 가 아님
                        jwtTokenUtil.getTokenUsage(
                            accessToken,
                            authJwtClaimsAes256InitializationVector,
                            authJwtClaimsAes256EncryptionKey
                        ).lowercase() != "access" || // 토큰 용도가 다름
                        // 남은 시간이 최대 만료시간을 초과 (서버 기준이 변경되었을 때, 남은 시간이 더 많은 토큰을 견제하기 위한 처리)
                        jwtTokenUtil.getRemainSeconds(accessToken) > authJwtAccessTokenExpirationTimeSec ||
                        jwtTokenUtil.getIssuer(accessToken) != authJwtIssuer || // 발행인 불일치
                        !jwtTokenUtil.validateSignature(
                            accessToken,
                            authJwtSecretKeyString
                        ) // 시크릿 검증이 무효 = 위변조 된 토큰
                    ) {
                        return null
                    }

                    // 토큰 만료 검증
                    val jwtRemainSeconds = jwtTokenUtil.getRemainSeconds(accessToken)
                    if (jwtRemainSeconds <= 0L) {
                        return null
                    }

                    // 회원 권한
                    val authorities: ArrayList<GrantedAuthority> = ArrayList()
                    for (memberRole in jwtTokenUtil.getRoleList(
                        accessToken,
                        authJwtClaimsAes256InitializationVector,
                        authJwtClaimsAes256EncryptionKey
                    )) {
                        authorities.add(
                            SimpleGrantedAuthority(memberRole)
                        )
                    }

                    return authorities
                }

                else -> {
                    return null
                }
            }
        }
    }


    // ----
    // [Session-Cookie 인증 체계 적용 필터 체인]
//    @Bean
//    @Order(2)
//    @CustomTransactional([Db1MainConfig.TRANSACTION_NAME], readOnly = true)
//    fun securityFilterChainSessionCookie(
//        http: HttpSecurity,
//        userDetailService: UserDetailsServiceMainSc,
//        db1RaillyLinkerCompanyTotalAuthMemberRepository: Db1_RaillyLinkerCompany_TotalAuthMember_Repository
//    ): SecurityFilterChain {
//        // !!!시큐리티 필터 추가시 수정!!!
//        // 본 시큐리티 필터가 관리할 주소 체계
//        val securityUrlList = listOf(
//            "/my-service/sc/**"
//        ) // 위 모든 경로에 적용
//
//        val securityMatcher = http.securityMatcher(*securityUrlList.toTypedArray())
//
//        securityMatcher.headers { headersCustomizer ->
//            // iframe 허용 설정
//            // 기본은 허용하지 않음, sameOrigin 은 같은 origin 일 때에만 허용하고, disable 은 모두 허용
//            headersCustomizer.frameOptions { frameOptionsConfig ->
//                frameOptionsConfig.sameOrigin()
//            }
//        }
//
//        // cors 적용(서로 다른 origin 의 웹화면에서 리퀘스트 금지)
//        securityMatcher.cors {}
//        // csrf 보안 설정
//        // HTML 에서 form 요청을 보낼 때,
//        // <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
//        // 이렇게 csrf 값을 같이 줘야하는데, Thymeleaf 에선 굳이 명시하지 않아도 자동으로 포함됩니다.
//        securityMatcher.csrf {}
//
//        // 스프링 시큐리티 로그인 설정
//        securityMatcher.formLogin { formLoginCustomizer ->
//            // 로그인이 필요한 요청을 했을 때 자동으로 이동할 로그인 화면 경로
//            // 이 경로를 만들어서 로그인 화면의 HTML 과 그 안의 form 태그 요소들을 만들어야 합니다.
//            formLoginCustomizer.loginPage("/my-service/sc/auth/login")
//            // 로그인 인증처리 경로
//            // 로그인 form 태그는 이 경로로 POST 요청을 보내야 하며,
//            // 이 경로에 대한 처리는 개발자가 따로 작성할 필요가 없이 자동으로 처리됩니다.
//            formLoginCustomizer.loginProcessingUrl("/my-service/sc/auth/login-process")
//            // 인증 성공 시 자동으로 이동하는 경로
////            formLoginCustomizer.defaultSuccessUrl("/")
//            // 정상 인증 성공 후 별도의 처리가 필요한 경우 커스텀 핸들러 생성하여 등록
//            formLoginCustomizer.successHandler { _, response, _ ->
//                response.sendRedirect("/")
//            }
//            // 인증 실패 시 자동으로 이동하는 경로
////            formLoginCustomizer.failureUrl("/main/sc/v1/login?fail")
//            // 인증 실패 후 별도의 처리가 필요한 경우 커스텀 핸들러를 생성하여 등록
//            formLoginCustomizer.failureHandler { request, response, exception ->
//                when (exception) {
//                    is SessionAuthenticationException -> {
//                        // 동시 접속 금지로 실패한 경우
//                        response.sendRedirect("/my-service/sc/auth/login?duplicated")
//                    }
//
//                    is LockedException -> {
//                        // 계정 정지로 인한 실패
//                        val userName = request.getParameter("username")
//                        val memberDataEntity: Db1_RaillyLinkerCompany_TotalAuthMember =
//                            getMemberEntity(userName, db1RaillyLinkerCompanyTotalAuthMemberRepository)
//                        response.sendRedirect("/main/sc/v1/login?lock=${memberDataEntity.uid}")
//                    }
//
//                    else -> {
//                        // 그외 인증 실패
//                        response.sendRedirect("/my-service/sc/auth/login?fail")
//                    }
//                }
//            }
//        }
//
//        // 커스텀 UserDetailsService 설정
//        securityMatcher.userDetailsService(userDetailService)
//
//        // 스프링 시큐리티 로그아웃 설정
//        securityMatcher.logout { logoutCustomizer ->
//            // 로그아웃(현 세션에서 로그인된 멤버 정보를 제거) 경로
//            logoutCustomizer.logoutUrl("/my-service/sc/auth/logout")
//            // 로그아웃 시 이동할 경로
////            logoutCustomizer.logoutSuccessUrl("/main/sc/v1/login?logout")
//            logoutCustomizer.logoutSuccessHandler { request, response, _ ->
//                // 로그아웃 시 현 위치 다시 호출
//                response.sendRedirect(request.getHeader("Referer") ?: "/")
//            }
//        }
//
//        // 시큐리티 예외 처리
//        securityMatcher.exceptionHandling { exceptionHandlingCustomizer ->
//            exceptionHandlingCustomizer.accessDeniedPage("/my-service/sc/error?type=ACCESS_DENIED") // 권한 부족 시 이동할 페이지 설정
//            // 또는 커스텀 핸들러 설정
//            // exceptionHandlingCustomizer.accessDeniedHandler { request, response, accessDeniedException ->
//            //     response.sendRedirect("/access-denied")
//            // }
//        }
//
//        // (API 요청 제한)
//        // 기본적으로 모두 Open
//        securityMatcher.authorizeHttpRequests { authorizeHttpRequestsCustomizer ->
////            authorizeHttpRequestsCustomizer.requestMatchers(
////                "/swagger-ui/**",
////                "/v3/api-docs/**",
////                "/v3/api-docs.yaml"
////            ).hasAnyRole(
////                "ADMIN",
////                "DEVELOPER",
////                "SERVER_DEVELOPER"
////            )
//
//            // 그외 모든 요청 허용
//            authorizeHttpRequestsCustomizer.anyRequest().permitAll()
//        }
//
//        securityMatcher.sessionManagement { sessionManagementCustomizer ->
//            sessionManagementCustomizer
//                // 세션 고정 공격 방지 : 로그인 할 때마다 새로운 세션 ID 를 발급받습니다.
//                .sessionFixation().migrateSession()
//                // 세션 동시 접속 개수 (-1 : 무한)
//                .maximumSessions(1)
//                // 세션 만료시 이동 경로
//                .expiredUrl("/my-service/sc/auth/login?expired")
//                // 세션 동시 접속 초과 동작 (true : 추가 로그인을 막음, false : 이전 세션을 만료시킴)
//                .maxSessionsPreventsLogin(false)
//                .sessionRegistry(sessionRegistry())
//        }
//
//        return securityMatcher.build()
//    }
//
//    @Service
//    class UserDetailsServiceMainSc(
//        private val db1RaillyLinkerCompanyTotalAuthMemberRepository: Db1_RaillyLinkerCompany_TotalAuthMember_Repository,
//        private val db1RaillyLinkerCompanyTotalAuthMemberRoleRepository: Db1_RaillyLinkerCompany_TotalAuthMemberRole_Repository,
//        private val db1NativeRepository: Db1_Native_Repository
//    ) : UserDetailsService {
//        companion object {
//            fun getMemberEntity(
//                userName: String,
//                db1RaillyLinkerCompanyTotalAuthMemberRepository: Db1_RaillyLinkerCompany_TotalAuthMember_Repository
//            ): Db1_RaillyLinkerCompany_TotalAuthMember {
//                // userName 은 {타입}_{아이디} 의 형태로 입력된다고 가정합니다.
//                // 예를들어 email 로그인의 test@test.com 계정의 로그인시에는,
//                // email_test@test.com 이라는 값이 userName 에 담겨져 올 것입니다.
//                val userNameSplitIdx = userName.indexOf('_')
//                if (userNameSplitIdx == -1) {
//                    throw UsernameNotFoundException("유효하지 않은 로그인 타입입니다. : ")
//                }
//
//                // 로그인 타입과 아이디 분리
//                val userNameType = userName.substring(0, userNameSplitIdx)
//                val userNameValue = userName.substring(userNameSplitIdx + 1)
//
//                val memberDataEntity: Db1_RaillyLinkerCompany_TotalAuthMember
//                when (userNameType) {
//                    // 아이디 로그인
//                    "accountId" -> {
//                        memberDataEntity =
//                            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByAccountIdAndRowDeleteDateStr(
//                                userNameValue,
//                                "/"
//                            )
//                                ?: throw UsernameNotFoundException("아이디 유저 정보가 존재하지 않습니다 : $userNameValue")
//                    }
//
//                    else -> {
//                        throw UsernameNotFoundException("유효하지 않은 로그인 타입입니다. : $userNameType")
//                    }
//                }
//                return memberDataEntity
//            }
//        }
//
//        override fun loadUserByUsername(userName: String): UserDetails {
//            // 로그인 타입별 멤버 정보 가져오기(없다면 UsernameNotFoundException)
//            val memberDataEntity: Db1_RaillyLinkerCompany_TotalAuthMember =
//                getMemberEntity(userName, db1RaillyLinkerCompanyTotalAuthMemberRepository)
//
//            // 회원 권한을 가져와 변환
//            val memberRoleDataEntityList =
//                db1RaillyLinkerCompanyTotalAuthMemberRoleRepository.findAllByTotalAuthMemberAndRowDeleteDateStr(
//                    memberDataEntity,
//                    "/"
//                )
//            val authorities: MutableCollection<GrantedAuthority> = memberRoleDataEntityList
//                .map { roleData -> SimpleGrantedAuthority(roleData.role) }
//                .toMutableList()
//
//            // 정지 여부 파악
//            val lockList =
//                db1NativeRepository.findAllNowActivateMemberLockInfo(
//                    memberDataEntity.uid!!,
//                    LocalDateTime.now()
//                )
//
//            // 이것이 반환된 후 비밀번호 검증까지 끝나면 이 데이터가 메모리에 저장되어 있습니다.
//            // api 에서는 @Parameter(hidden = true) principal: Principal? 이것으로 받은 후,
//            // principal 이 null 이라면 로그아웃 상태, principal 이 있다면 principal?.name 으로 현재 로그인한 회원 정보를 알 수 있습니다.
//            // 주의사항으로, 세션 메모리에 저장된 UserDetails 객체는 세션 지속 시간때까지 불변입니다.
//            // 예를들어 현재 Admin 권한인 상태로 로그인을 한 상태라면, 동적으로 Admin 권한이 데이터베이스에서 사라졌어도 admin 전용 api 에 호출할 수 있습니다.
//            return UserDetailsVo(
//                // UserDetail 의 userName 은 user 고유번호로 대체합니다.
//                memberDataEntity.uid!!,
//                // 암호화되어 데이터베이스에 저장된 비밀번호
//                memberDataEntity.accountPassword!!,
//                authorities,
//                lockList.isNotEmpty()
//            )
//        }
//
//        class UserDetailsVo(
//            private val memberUid: Long,
//            private val password: String,
//            private val authorities: MutableCollection<out GrantedAuthority>,
//            private val memberLock: Boolean
//        ) : UserDetails {
//            override fun equals(other: Any?): Boolean {
//                // maximumSessions 설정을 위한 오버라이드
//                if (other is UserDetailsVo) {
//                    return this.username == other.username
//                }
//                return false
//            }
//
//            override fun hashCode(): Int {
//                // maximumSessions 설정을 위한 오버라이드
//                return this.username.hashCode()
//            }
//
//            override fun getUsername(): String {
//                return memberUid.toString()
//            }
//
//            override fun getPassword(): String {
//                return password
//            }
//
//            override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
//                return authorities
//            }
//
//            override fun isAccountNonExpired(): Boolean {
//                return true
//            }
//
//            override fun isAccountNonLocked(): Boolean {
//                return !memberLock
//            }
//
//            override fun isCredentialsNonExpired(): Boolean {
//                return true
//            }
//
//            override fun isEnabled(): Boolean {
//                return true
//            }
//        }
//    }
}