package com.raillylinker.util_components

import org.springframework.stereotype.Component

// [커스텀 유틸 함수 모음]
@Component
class CustomUtil {
    // (byteArray 를 Hex String 으로 반환)
    fun bytesToHex(bytes: ByteArray): String {
        val builder = StringBuilder()
        for (b in bytes) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString()
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}