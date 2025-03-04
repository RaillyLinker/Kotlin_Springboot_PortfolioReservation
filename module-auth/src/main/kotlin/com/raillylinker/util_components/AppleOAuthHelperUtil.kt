package com.raillylinker.util_components

import com.raillylinker.retrofit2_classes.RepositoryNetworkRetrofit2
import com.raillylinker.retrofit2_classes.request_apis.AppleIdAppleComRequestApi
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.boot.json.BasicJsonParser
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.*

// [Apple OAuth2 검증 관련 유틸]
@Component
class AppleOAuthHelperUtil(private val cryptoUtil: CryptoUtil) {
    // Retrofit2 요청 객체
    private val networkRetrofit2 = RepositoryNetworkRetrofit2.getInstance()

    // (애플 Id Token 검증 함수)
    // 검증이 완료되었다면 프로필 정보가 반환되고, 검증되지 않는다면 null 반환
    fun getAppleMemberData(idToken: String): AppleProfileData? {
        try {
            // 공개키 가져오기
            val response = networkRetrofit2.appleIdAppleComRequestApi.getAuthKeys().execute()

            if (response.body() == null) {
                return null
            }

            val testEntityVoList = response.body()!!.keys

            // idToken 헤더의 암호화 알고리즘 정보 가져오기
            val header = cryptoUtil.base64Decode(idToken.split(".")[0])
            val headerMap = BasicJsonParser().parseMap(header)

            val idTokenKid = headerMap["kid"].toString()
            val idTokenAlg = headerMap["alg"].toString()

            // 공개키 리스트를 순회하며 암호화 알고리즘이 동일한 키 객체 가져오기
            var appleKeyObject: AppleIdAppleComRequestApi.GetAuthKeysOutputVo.Key? = null
            for (idx in testEntityVoList.indices) {
                val jsonObject = testEntityVoList[idx]
                val kid = jsonObject.kid
                val alg = jsonObject.alg

                if (Objects.equals(idTokenKid, kid) && Objects.equals(idTokenAlg, alg)) {
                    appleKeyObject = jsonObject
                    break
                }
            }

            // 일치하는 공개키가 없음
            if (appleKeyObject == null) {
                return null
            }

            val nBytes: ByteArray = Base64.getUrlDecoder().decode(appleKeyObject.n)
            val eBytes: ByteArray = Base64.getUrlDecoder().decode(appleKeyObject.e)

            val n = BigInteger(1, nBytes)
            val e = BigInteger(1, eBytes)

            val publicKeySpec = RSAPublicKeySpec(n, e)
            val keyFactory = KeyFactory.getInstance(appleKeyObject.kty)
            val publicKey = keyFactory.generatePublic(publicKeySpec)

            val jwtParser = Jwts.parser()
            jwtParser.verifyWith(publicKey)

            val memberInfo: Claims = jwtParser.build().parseSignedClaims(idToken).payload

            return AppleProfileData(
                memberInfo["sub"] as String,
                if (memberInfo.containsKey("is_private_email") && memberInfo["is_private_email"] == "true") {
                    null
                } else {
                    memberInfo["email"] as String
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    data class AppleProfileData(
        val snsId: String,
        val email: String?
    )
}