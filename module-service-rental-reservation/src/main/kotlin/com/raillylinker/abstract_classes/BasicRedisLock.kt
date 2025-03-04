package com.raillylinker.abstract_classes

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import java.util.*

// [RedisLock 의 Abstract 클래스]
abstract class BasicRedisLock(
    private val redisTemplateObj: RedisTemplate<String, String>,
    private val mapName: String
) {

    // <공개 메소드 공간>
    // (락 획득 메소드 - Lua 스크립트 적용)
    fun tryLock(
        // Lock 키(실제 Redis 에 저장되는 키는 "$mapName:${key}" 이러한 형태입니다.)
        key: String,
        // Lock 만료 시간(이 시간이 지나면 자동으로 락 정보가 사라집니다.)
        expireTimeMs: Long
    ): String? {
        val uuid = UUID.randomUUID().toString()

        // Redis Storage 에 실제로 저장 되는 키 (map 이름과 키를 합친 String)
        val innerKey = "$mapName:${key}" // 실제 저장되는 키 = 그룹명:키

        val scriptResult = if (expireTimeMs < 0) {
            // 만료시간 무한
            redisTemplateObj.execute(
                RedisScript.of(
                    """
                        if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then
                            return 1
                        else
                            return 0
                        end
                    """.trimIndent(),
                    Long::class.java
                ),
                listOf(innerKey),
                uuid
            )
        } else {
            // 만료시간 유한
            redisTemplateObj.execute(
                RedisScript.of(
                    """
                        if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then
                            redis.call('pexpire', KEYS[1], ARGV[2])
                            return 1
                        else
                            return 0
                        end
                    """.trimIndent(),
                    Long::class.java
                ),
                listOf(innerKey),
                uuid,
                expireTimeMs.toString()
            )
        }

        return if (scriptResult == 1L) {
            // 락을 성공적으로 획득한 경우
            uuid
        } else {
            // 락을 획득하지 못한 경우
            null
        }
    }


    // ----
    // (락 해제 메소드 - Lua 스크립트 적용)
    fun unlock(
        // tryLock 에 사용한 Lock 키
        key: String,
        // tryLock 에서 발행한 uuid
        uuid: String
    ) {
        // Redis Storage 에 실제로 저장 되는 키 (map 이름과 키를 합친 String)
        val innerKey = "$mapName:${key}" // 실제 저장되는 키 = 그룹명:키

        redisTemplateObj.execute(
            RedisScript.of(
                """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                """.trimIndent(),
                Long::class.java
            ),
            listOf(innerKey),
            uuid
        )
    }


    // ----
    // (락 강제 삭제)
    // 만료시간과 uuid 에 상관없이 무조건적으로 락을 삭제합니다.
    fun deleteLock(
        key: String
    ) {
        val innerKey = "$mapName:${key}" // 실제 저장되는 키 = 그룹명:키

        redisTemplateObj.delete(innerKey)
    }


    // ----
    // (반복 락 획득 시도)
    // 락을 획득 할 때까지 반복적으로 tryLock 을 하고, 락 획득시 콜백을 실행합니다.
    fun <OutputType> tryLockRepeat(
        // lock 키
        key: String,
        // lock 만료시간
        // 음수일 경우 무한 대기
        // whenLockPass 의 작업 수행 시간보다 커야하며, 작다면 작업을 수행하는 도중 락이 풀릴 것입니다.
        expireTimeMs: Long,
        // lock 을 얻으면 수행할 작업
        whenLockPass: () -> OutputType,
        // lock 불통과시 기본 대기시간
        baseWaitTime: Long = 50L,
        // lock 불통과 때마다 기본 대기시간이 증가하는 비율
        incrementalFactor: Double = 0.1,
        // 대기 시간 증가 최대값
        maxWaitTime: Long = 100L,
    ): OutputType {
        // 공유 락 해제 키
        var unLockKey: String? = null
        // 현재 재시도 횟수
        var attempt: Long = 0

        while (unLockKey == null) {
            // 공유 락을 얻을 때 까지 반복
            unLockKey = this.tryLock(key, expireTimeMs)
            if (unLockKey == null) {
                // 공유 락을 못 얻었다면 대기 시간 증가(불발 횟수에 따라 대기 시간 증가 처리)
                val calcWaitTime = baseWaitTime + (baseWaitTime * attempt * incrementalFactor).toLong()

                if (attempt < Long.MAX_VALUE) {
                    // 재시도 횟수 오버플로우 방지
                    attempt++
                }

                val waitTime = if (calcWaitTime > maxWaitTime) {
                    maxWaitTime
                } else if (calcWaitTime < baseWaitTime) {
                    baseWaitTime
                } else {
                    calcWaitTime
                }

                Thread.sleep(waitTime)
            }
        }

        val output: OutputType

        try {
            output = whenLockPass()
        } finally {
            // 작업 완료로 인한 락 반납
            this.unlock(key, unLockKey)
        }

        return output
    }
}