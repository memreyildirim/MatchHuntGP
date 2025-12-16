package com.emreyildirim.matchhuntv1.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun `valid password returns success and no errors`() {
        val result = PasswordValidator.validate("Valid123")

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `password missing uppercase is invalid`() {
        val result = PasswordValidator.validate("lowercase1")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("büyük harf", ignoreCase = true) })
    }

    @Test
    fun `too short password reports length error`() {
        val result = PasswordValidator.validate("Aa1")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("karakter", ignoreCase = true) })
    }

    @Test
    fun `password missing number is invalid`() {
        val result = PasswordValidator.validate("NoNumberX")

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("rakam", ignoreCase = true) })
    }

    @Test
    fun `multiple issues accumulate all error messages`() {
        val result = PasswordValidator.validate("short")

        assertFalse(result.isValid)
        assertEquals(3, result.errors.size) // eksik büyük, küçük veya sayı durumuna göre güncellenebilir
    }
}








