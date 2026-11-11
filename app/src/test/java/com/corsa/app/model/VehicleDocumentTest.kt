package com.corsa.app.model

import org.junit.Assert.*
import org.junit.Test

class VehicleDocumentTest {
    @Test fun datesAreStrictAndWarningsIncludeExactBoundaries() {
        assertNull(parseDocumentDate("2026-02-30"))
        assertNull(parseDocumentDate("2026-1-01"))
        val document = VehicleDocument(1, 1, "Insurance", DocumentType.INSURANCE, null, null,
            "2026-12-12", 0, null, null, null)
        assertEquals(DocumentExpiration.EXPIRING_SOON, document.expiration("2026-11-12"))
        assertEquals(DocumentExpiration.CURRENT, document.expiration("2026-11-11"))
        assertEquals(DocumentExpiration.EXPIRES_TODAY, document.expiration("2026-12-12"))
        assertEquals(DocumentExpiration.EXPIRED, document.expiration("2026-12-13"))
    }
}
