package com.raillylinker.kafka_components.consumers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Kafka1MainConsumer {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        // !!!모듈 컨슈머 그룹 아이디!!!
        private const val CONSUMER_GROUP_ID = "com.raillylinker.auth"
    }

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
}