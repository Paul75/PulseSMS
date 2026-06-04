package com.skeler.pulse.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpCodeExtractorTest {

    @Test
    fun should_extract_four_to_eight_digit_code_when_body_contains_otp_context() {
        val code = OtpCodeExtractor.extractCode("Pulse verification code: 493827. Do not share it.")

        assertEquals("493827", code)
    }

    @Test
    fun should_prefer_otp_code_when_body_also_contains_amount() {
        val code = OtpCodeExtractor.extractCode("Pay USD 1299. Your bank OTP is 804112.")

        assertEquals("804112", code)
    }

    @Test
    fun should_extract_business_account_verification_code() {
        val code = OtpCodeExtractor.extractCode("Your business account code is 739201.")

        assertEquals("739201", code)
    }

    @Test
    fun should_ignore_amount_when_no_otp_context_exists() {
        val code = OtpCodeExtractor.extractCode("Your card was charged $1299 at Store.")

        assertNull(code)
    }

    @Test
    fun should_ignore_time_when_no_otp_context_exists() {
        val code = OtpCodeExtractor.extractCode("Reminder: appointment time 0930 AM tomorrow.")

        assertNull(code)
    }

    @Test
    fun should_ignore_date_when_no_otp_context_exists() {
        val code = OtpCodeExtractor.extractCode("Your delivery date is 20260529.")

        assertNull(code)
    }
}
