package com.roomfit.room;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * List<String>을 단일 delimited 컬럼으로 저장하는 컨버터.
 * Furniture는 Room/Layout의 @ElementCollection 안에 들어가는 @Embeddable이라
 * styleTags(List<String>)를 그 안에서 또 다른 @ElementCollection으로 매핑할 수 없다
 * (JPA는 @ElementCollection 중첩을 지원하지 않음) — 그래서 콤마 구분 문자열로 평탄화한다.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return String.join(DELIMITER, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dbData.split(DELIMITER)).filter(value -> !value.isBlank()).toList();
    }
}
