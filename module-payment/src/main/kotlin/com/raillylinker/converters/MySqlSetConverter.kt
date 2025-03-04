package com.raillylinker.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

// [JPA 에서 Set<String> 타입을 입출력하기 위한 컨버터]
@Converter
class MySqlSetConverter : AttributeConverter<Set<String>?, String> {
    override fun convertToDatabaseColumn(attribute: Set<String>?): String? {
        return attribute?.joinToString(",")
    }

    override fun convertToEntityAttribute(dbData: String?): Set<String>? {
        return dbData?.split(",")?.toSet()
    }
}