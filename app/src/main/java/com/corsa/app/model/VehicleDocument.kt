package com.corsa.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class DocumentType { REGISTRATION, INSURANCE, SERVICE_RECORD, OTHER }

data class VehicleDocument(
    val id: Long, val vehicleId: Long, val title: String, val documentType: DocumentType,
    val documentNumber: String?, val issueDate: String?, val expirationDate: String?,
    val costMinor: Long, val filePath: String?, val physicalLocation: String?, val description: String?
)

data class DocumentDraft(
    val id: Long = 0, val vehicleId: Long, val title: String = "",
    val documentType: DocumentType = DocumentType.REGISTRATION,
    val documentNumber: String = "", val issueDate: String = "", val expirationDate: String = "",
    val cost: String = "", val filePath: String? = null,
    val physicalLocation: String = "", val description: String = ""
)

enum class DocumentExpiration { CURRENT, EXPIRING_SOON, EXPIRES_TODAY, EXPIRED }

fun parseDocumentDate(value: String): Date? {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)
    } catch (_: java.text.ParseException) { null }
}

fun VehicleDocument.expiration(today: String): DocumentExpiration {
    val end = expirationDate?.let(::parseDocumentDate) ?: return DocumentExpiration.CURRENT
    val now = requireNotNull(parseDocumentDate(today))
    val days = (end.time - now.time) / 86_400_000
    return when {
        days < 0 -> DocumentExpiration.EXPIRED
        days == 0L -> DocumentExpiration.EXPIRES_TODAY
        days <= 30 -> DocumentExpiration.EXPIRING_SOON
        else -> DocumentExpiration.CURRENT
    }
}
