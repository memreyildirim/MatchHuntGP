package com.emreyildirim.matchhuntv1.utils

class PasswordValidator {
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val UPPERCASE_REGEX = Regex("[A-Z]")
        private val LOWERCASE_REGEX = Regex("[a-z]")
        private val NUMBER_REGEX = Regex("[0-9]")


        fun validate(password: String): PasswordValidationResult {
            val errors = mutableListOf<String>()

            if (password.length < MIN_PASSWORD_LENGTH) {
                errors.add("Şifre en az $MIN_PASSWORD_LENGTH karakter olmalıdır")
            }
            if (!password.contains(UPPERCASE_REGEX)) {
                errors.add("Şifre en az bir büyük harf içermelidir")
            }
            if (!password.contains(LOWERCASE_REGEX)) {
                errors.add("Şifre en az bir küçük harf içermelidir")
            }
            if (!password.contains(NUMBER_REGEX)) {
                errors.add("Şifre en az bir rakam içermelidir")
            }

            return if (errors.isEmpty()) {
                PasswordValidationResult(true)
            } else {
                PasswordValidationResult(false, errors)
            }
        }
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) 