package com.raillylinker

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ModuleTest {
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test() {
        classLogger.info("test")
    }
}