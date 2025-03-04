package com.raillylinker.redis_map_components.redis1_main

import com.raillylinker.abstract_classes.BasicRedisMap
import com.raillylinker.configurations.redis_configs.Redis1MainConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

// [RedisMap 컴포넌트]
// 통합 로그인 JWT 만료 토큰 정보
// 기존 발행 토큰을 재 심사 하기 위해 만료시키려면 이곳에 입력하세요.
// 키로는 액세스 토큰만을 넣는 것이 아니라 토큰 타입을 합쳐서 "Bearer_tes123t4access16token3" 이런 값을 넣습니다.
@Component
class Redis1_Map_TotalAuthForceExpireAuthorizationSet(
    // !!!RedisConfig 종류 변경!!!
    @Qualifier(Redis1MainConfig.REDIS_TEMPLATE_NAME) val redisTemplate: RedisTemplate<String, String>
) : BasicRedisMap<Redis1_Map_TotalAuthForceExpireAuthorizationSet.ValueVo>(redisTemplate, MAP_NAME, ValueVo::class.java) {
    // <멤버 변수 공간>
    companion object {
        // !!!중복되지 않도록, 본 클래스명을 MAP_NAME 으로 설정하기!!!
        const val MAP_NAME = "Redis1_Map_TotalAuthForceExpireAuthorizationSet"
    }

    // !!!본 RedisMAP 의 Value 클래스 설정!!!
    class ValueVo
}