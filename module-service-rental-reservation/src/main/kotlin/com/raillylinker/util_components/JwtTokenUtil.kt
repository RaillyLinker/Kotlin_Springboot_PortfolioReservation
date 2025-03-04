package com.raillylinker.util_components

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.boot.json.BasicJsonParser
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// [JWT 토큰 유틸]
@Component
class JwtTokenUtil(private val cryptoUtil: CryptoUtil) {
    // <공개 메소드 공간>
    // (액세스 토큰 발행)
    // memberRoleList : 멤버 권한 리스트 (ex : ["ROLE_ADMIN", "ROLE_DEVELOPER"])
    fun generateAccessToken(
        memberUid: Long,
        accessTokenExpirationTimeSec: Long,
        jwtClaimsAes256InitializationVector: String,
        jwtClaimsAes256EncryptionKey: String,
        issuer: String,
        jwtSecretKeyString: String,
        roleList: List<String>
    ): String {
        return doGenerateToken(
            memberUid,
            "access",
            accessTokenExpirationTimeSec,
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey,
            issuer,
            jwtSecretKeyString,
            roleList
        )
    }

    // (리프레시 토큰 발행)
    fun generateRefreshToken(
        memberUid: Long,
        refreshTokenExpirationTimeSec: Long,
        jwtClaimsAes256InitializationVector: String,
        jwtClaimsAes256EncryptionKey: String,
        issuer: String,
        jwtSecretKeyString: String
    ): String {
        return doGenerateToken(
            memberUid,
            "refresh",
            refreshTokenExpirationTimeSec,
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey,
            issuer,
            jwtSecretKeyString,
            null
        )
    }

    // (JWT Secret 확인)
    // : 토큰 유효성 검증. 유효시 true, 위변조시 false
    fun validateSignature(
        token: String,
        jwtSecretKeyString: String
    ): Boolean {
        val tokenSplit = token.split(".")
        val header = tokenSplit[0]
        val payload = tokenSplit[1]
        val signature = tokenSplit[2]

        // base64 로 인코딩된 header 와 payload 를 . 로 묶은 후 이를 시크릿으로 HmacSha256 해싱을 적용하여 signature 를 생성
        val newSig = cryptoUtil.hmacSha256("$header.$payload", jwtSecretKeyString)

        // 위 방식으로 생성된 signature 가 token 으로 전달된 signature 와 동일하다면 위/변조되지 않은 토큰으로 판단 가능
        // = 발행시 사용한 시크릿과 검증시 사용된 시크릿이 동일
        return signature == newSig
    }

    // (JWT 정보 반환)
    // Member Uid
    fun getMemberUid(
        token: String,
        jwtClaimsAes256InitializationVector: String,
        jwtClaimsAes256EncryptionKey: String
    ): Long {
        return cryptoUtil.decryptAES256(
            parseJwtForPayload(token)["mu"].toString(),
            "AES/CBC/PKCS5Padding",
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey
        ).toLong()
    }

    // (Token 용도 (access or refresh) 반환)
    fun getTokenUsage(
        token: String,
        jwtClaimsAes256InitializationVector: String,
        jwtClaimsAes256EncryptionKey: String
    ): String {
        return cryptoUtil.decryptAES256(
            parseJwtForPayload(token)["tu"].toString(),
            "AES/CBC/PKCS5Padding",
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey
        )
    }

    // (멤버 권한 리스트 반환)
    fun getRoleList(
        token: String,
        jwtClaimsAes256InitializationVector: String,
        jwtClaimsAes256EncryptionKey: String
    ): List<String> {
        val rl = cryptoUtil.decryptAES256(
            parseJwtForPayload(token)["rl"].toString(),
            "AES/CBC/PKCS5Padding",
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey
        )
        return Gson().fromJson(rl, object : TypeToken<List<String>>() {}.type)
    }

    // (발행자 반환)
    fun getIssuer(token: String): String {
        return parseJwtForPayload(token)["iss"].toString()
    }

    // (토큰 남은 유효 시간(초) 반환 (만료된 토큰이라면 0))
    fun getRemainSeconds(token: String): Long {
        val exp = parseJwtForPayload(token)["exp"] as Long
        val currentEpochSeconds = Instant.now().epochSecond

        return if (currentEpochSeconds < exp) exp - currentEpochSeconds else 0
    }

    // (토큰 만료 일시 반환)
    fun getExpirationDateTime(token: String): LocalDateTime {
        val exp = parseJwtForPayload(token)["exp"] as Long
        val expirationInstant = Instant.ofEpochSecond(exp)

        return LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault())
    }

    // (토큰 타입)
    fun getTokenType(token: String): String {
        return parseJwtForHeader(token)["typ"].toString()
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (JWT 토큰 생성)
    private fun doGenerateToken(
        memberUid: Long,
        tokenUsage: String,
        expireTimeSec: Long,
        jwtClaimsAes256InitializationVector: String,
        jwtClaimsAes256EncryptionKey: String,
        issuer: String,
        jwtSecretKeyString: String,
        roleList: List<String>?
    ): String {
        val jwtBuilder = Jwts.builder()

        val headersMap = mutableMapOf<String, Any>()

        headersMap["typ"] = "JWT"

        jwtBuilder.header().empty().add(headersMap)

        val claimsMap = mutableMapOf<String, Any>()

        // member uid
        claimsMap["mu"] = cryptoUtil.encryptAES256(
            memberUid.toString(),
            "AES/CBC/PKCS5Padding",
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey
        )

        // 멤버 권한 리스트
        if (roleList != null) {
            claimsMap["rl"] = cryptoUtil.encryptAES256(
                Gson().toJson(roleList),
                "AES/CBC/PKCS5Padding",
                jwtClaimsAes256InitializationVector,
                jwtClaimsAes256EncryptionKey
            )
        }

        // token usage
        claimsMap["tu"] = cryptoUtil.encryptAES256(
            tokenUsage,
            "AES/CBC/PKCS5Padding",
            jwtClaimsAes256InitializationVector,
            jwtClaimsAes256EncryptionKey
        )

        // 발행자
        claimsMap["iss"] = issuer

        claimsMap["iat"] = Instant.now().epochSecond
        claimsMap["exp"] = Instant.now().epochSecond + expireTimeSec
        claimsMap["cd"] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSSSSS"))

        jwtBuilder.claims().empty().add(claimsMap)

        jwtBuilder
            .signWith(
                Keys.hmacShaKeyFor(jwtSecretKeyString.toByteArray(StandardCharsets.UTF_8)),
                Jwts.SIG.HS256
            )

        return jwtBuilder.compact()
    }

    // (base64 로 인코딩된 Header, Payload 를 base64 로 디코딩)
    private fun parseJwtForHeader(jwt: String): Map<String, Any> {
        val header = cryptoUtil.base64Decode(jwt.split(".")[0])
        return BasicJsonParser().parseMap(header)
    }

    private fun parseJwtForPayload(jwt: String): Map<String, Any> {
        val payload = cryptoUtil.base64Decode(jwt.split(".")[1])
        return BasicJsonParser().parseMap(payload)
    }
}