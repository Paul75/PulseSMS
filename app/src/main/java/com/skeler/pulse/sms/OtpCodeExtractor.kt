package com.skeler.pulse.sms

import java.time.DateTimeException
import java.time.LocalDate

internal object OtpCodeExtractor {
    private val codePattern = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")
    private val otpContextPattern = Regex(
        pattern = "\\b(otp|code|passcode|verification|verify|login|2fa|pin|auth|security|business|bank|account|one-time|onetime)\\b",
        option = RegexOption.IGNORE_CASE,
    )
    private val amountContextPattern = Regex(
        pattern = "\\b(usd|eur|gbp|inr|aed|cad|aud|amount|balance|total|paid|payment|charged|due)\\b",
        option = RegexOption.IGNORE_CASE,
    )
    private val timeContextPattern = Regex(
        pattern = "\\b(time|appointment|meeting|reminder|at|by|before|after)\\b",
        option = RegexOption.IGNORE_CASE,
    )
    private val amPmPattern = Regex("\\b(am|pm)\\b", RegexOption.IGNORE_CASE)
    private val currencySymbols = setOf('$', '€', '£', '¥', '₹')

    fun extractCode(body: String): String? {
        val candidates = codePattern.findAll(body)
            .map { match -> OtpCandidate(match.value, match.range.first, match.range.last + 1) }
            .toList()

        return candidates.firstOrNull { candidate ->
            hasOtpContext(body, candidate) && !isExcluded(body, candidate)
        }?.code ?: candidates.firstOrNull { candidate ->
            !isExcluded(body, candidate)
        }?.code
    }

    private fun hasOtpContext(body: String, candidate: OtpCandidate): Boolean =
        otpContextPattern.containsMatchIn(body.windowAround(candidate, radius = 24))

    private fun isExcluded(body: String, candidate: OtpCandidate): Boolean =
        isDateOrTime(body, candidate) ||
            isLikelyAmount(body, candidate)

    private fun isDateOrTime(body: String, candidate: OtpCandidate): Boolean =
        isDatePart(body, candidate) ||
            isLikelyCompactDate(candidate.code) ||
            isLikelyTime(body, candidate)

    private fun isDatePart(body: String, candidate: OtpCandidate): Boolean =
        body.isDateSeparatorBefore(candidate.start) ||
            body.isDateSeparatorAfter(candidate.end)

    private fun isLikelyAmount(body: String, candidate: OtpCandidate): Boolean {
        val before = body.trimmedWindow(candidate.start - 8, candidate.start)
        val after = body.trimmedWindow(candidate.end, candidate.end + 8)
        return before.lastOrNull() in currencySymbols ||
            after.firstOrNull() in currencySymbols ||
            amountContextPattern.containsMatchIn("$before $after")
    }

    private fun isLikelyTime(body: String, candidate: OtpCandidate): Boolean {
        if (candidate.code.length != 4 || !candidate.code.isValidHourMinute()) return false

        val context = body.windowAround(candidate, radius = 16)
        return timeContextPattern.containsMatchIn(context) || amPmPattern.containsMatchIn(context)
    }

    private fun isLikelyCompactDate(code: String): Boolean =
        code.length == 8 &&
            (code.isDateOf(::parseYearMonthDay) || code.isDateOf(::parseMonthDayYear) || code.isDateOf(::parseDayMonthYear))

    private fun String.isDateOf(parser: (String) -> LocalDate): Boolean =
        try {
            parser(this)
            true
        } catch (_: DateTimeException) {
            false
        } catch (_: NumberFormatException) {
            false
        }

    private fun parseYearMonthDay(code: String): LocalDate =
        LocalDate.of(code.substring(0, 4).toInt(), code.substring(4, 6).toInt(), code.substring(6, 8).toInt())

    private fun parseMonthDayYear(code: String): LocalDate =
        LocalDate.of(code.substring(4, 8).toInt(), code.substring(0, 2).toInt(), code.substring(2, 4).toInt())

    private fun parseDayMonthYear(code: String): LocalDate =
        LocalDate.of(code.substring(4, 8).toInt(), code.substring(2, 4).toInt(), code.substring(0, 2).toInt())

    private fun String.isValidHourMinute(): Boolean {
        val hour = substring(0, 2).toInt()
        val minute = substring(2, 4).toInt()
        return hour in 0..23 && minute in 0..59
    }

    private fun String.charBefore(index: Int): Char? = getOrNull(index - 1)

    private fun String.charAfter(index: Int): Char? = getOrNull(index)

    private fun String.isDateSeparatorBefore(index: Int): Boolean =
        charBefore(index) in dateSeparators && getOrNull(index - 2)?.isDigit() == true

    private fun String.isDateSeparatorAfter(index: Int): Boolean =
        charAfter(index) in dateSeparators && getOrNull(index + 1)?.isDigit() == true

    private fun String.windowAround(candidate: OtpCandidate, radius: Int): String =
        trimmedWindow(candidate.start - radius, candidate.end + radius)

    private fun String.trimmedWindow(start: Int, end: Int): String =
        substring(start.coerceAtLeast(0), end.coerceAtMost(length)).trim()
}

private val dateSeparators = setOf('/', '-', '.')

private data class OtpCandidate(
    val code: String,
    val start: Int,
    val end: Int,
)
