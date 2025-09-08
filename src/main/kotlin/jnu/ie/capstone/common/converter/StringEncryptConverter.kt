package jnu.ie.capstone.common.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import jnu.ie.capstone.common.security.util.AES256Util

@Converter
class StringEncryptConverter(private val util: AES256Util) : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.let { util.encrypt(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.let { util.decrypt(it) }
    }
}