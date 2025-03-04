package com.raillylinker.abstract_classes

import com.google.gson.Gson
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.TimeUnit

// [RedisMap 의 Abstract 클래스]
// 본 추상 클래스를 상속받은 클래스를 key, value, expireTime 및 Redis 저장, 삭제, 조회 기능 메소드를 가진 클래스로 만들어줍니다.
// Redis Storage 를 Map 타입처럼 사용 가능하도록 래핑해주는 역할을 합니다.
abstract class BasicRedisMap<ValueVo>(
    private val redisTemplateObj: RedisTemplate<String, String>,
    private val mapName: String,
    private val clazz: Class<ValueVo>
) {
    private val gson = Gson()

    // <공개 메소드 공간>
    // (RedisMap 에 Key-Value 저장)
    fun saveKeyValue(
        key: String,
        value: ValueVo,
        expireTimeMs: Long?
    ) {
        // 입력 키 검증
        validateKey(key)

        // Redis Storage 에 실제로 저장 되는 키 (map 이름과 키를 합친 String)
        val innerKey = "$mapName:${key}" // 실제 저장되는 키 = 그룹명:키

        // Redis Storage 에 실제로 저장 되는 Value (Json String 형식)
        redisTemplateObj.opsForValue().set(innerKey, gson.toJson(value))

        if (expireTimeMs != null) {
            // Redis Key 에 대한 만료시간 설정
            redisTemplateObj.expire(innerKey, expireTimeMs, TimeUnit.MILLISECONDS)
        }
    }

    // (RedisMap 의 모든 Key-Value 리스트 반환)
    fun findAllKeyValues(): List<RedisMapDataVo<ValueVo>> {
        val resultList = ArrayList<RedisMapDataVo<ValueVo>>()

        val keySet: Set<String> = redisTemplateObj.keys("$mapName:*")

        for (innerKey in keySet) {
            // innerKey : Redis Storage 에 실제로 저장 되는 키 (map 이름과 키를 합친 String)

            // 외부적으로 사용되는 Key (innerKey 에서 map 이름을 제거한 String)
            val key = innerKey.substring("$mapName:".length) // 키

            // Redis Storage 에 실제로 저장 되는 Value (Json String 형식)
            val innerValue = redisTemplateObj.opsForValue()[innerKey] ?: continue // 값

            // 외부적으로 사용되는 Value (Json String 을 테이블 객체로 변환)
            val valueObject = gson.fromJson(
                innerValue, // 해석하려는 json 형식의 String
                clazz // 파싱할 데이터 객체 타입
            )

            resultList.add(
                RedisMapDataVo(
                    key,
                    valueObject,
                    redisTemplateObj.getExpire(innerKey, TimeUnit.MILLISECONDS) // 남은 만료시간
                )
            )
        }

        return resultList
    }

    // (RedisMap 의 key-Value 를 반환)
    fun findKeyValue(
        key: String
    ): RedisMapDataVo<ValueVo>? {
        // 입력 키 검증
        validateKey(key)

        // Redis Storage 에 실제로 저장 되는 키 (map 이름과 키를 합친 String)
        val innerKey = "$mapName:$key"

        // Redis Storage 에 실제로 저장 되는 Value (Json String 형식)
        val innerValue = redisTemplateObj.opsForValue()[innerKey] // 값

        return if (innerValue == null) {
            null
        } else {
            // 외부적으로 사용되는 Value (Json String 을 테이블 객체로 변환)
            val valueObject = gson.fromJson(
                innerValue, // 해석하려는 json 형식의 String
                clazz // 파싱할 데이터 객체 타입
            )
            RedisMapDataVo(
                key,
                valueObject,
                redisTemplateObj.getExpire(innerKey, TimeUnit.MILLISECONDS) // 남은 만료시간
            )
        }
    }

    // (RedisMap 의 모든 Key-Value 리스트 삭제)
    fun deleteAllKeyValues() {
        val keySet: Set<String> = redisTemplateObj.keys("$mapName:*")

        redisTemplateObj.delete(keySet)
    }

    // (RedisMap 의 Key-Value 를 삭제)
    fun deleteKeyValue(
        key: String
    ) {
        // 입력 키 검증
        validateKey(key)

        // Redis Storage 에 실제로 저장 되는 키 (map 이름과 키를 합친 String)
        val innerKey = "$mapName:$key"

        redisTemplateObj.delete(innerKey)
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (입력 키 검증 함수)
    private fun validateKey(key: String) {
        if (key.trim().isEmpty()) {
            throw RuntimeException("key 는 비어있을 수 없습니다.")
        }
        if (key.contains(":")) {
            throw RuntimeException("key 는 :를 포함할 수 없습니다.")
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // [RedisMap 의 출력값 데이터 클래스]
    data class RedisMapDataVo<ValueVo>(
        val key: String, // 멤버가 입력한 키 : 실제 키는 ${groupName:key}
        val value: ValueVo,
        val expireTimeMs: Long // 남은 만료 시간 밀리초
    )
}