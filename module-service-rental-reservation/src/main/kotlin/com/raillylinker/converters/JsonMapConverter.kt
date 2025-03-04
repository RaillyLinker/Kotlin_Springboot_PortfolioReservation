package com.raillylinker.converters

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

// [JPA 에서 JSON 타입을 Map<String, Any?>? 타입으로 입출력하기 위한 컨버터]
@Converter
class JsonMapConverter : AttributeConverter<Map<String, Any?>?, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Any?>?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any?>? {
        return dbData?.let { objectMapper.readValue(it, Map::class.java) as Map<String, Any?> }
    }
}
