package com.example.adventcalendar.config;

import com.example.adventcalendar.util.EncryptionUtils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return EncryptionUtils.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return EncryptionUtils.decrypt(dbData);
	}
}
