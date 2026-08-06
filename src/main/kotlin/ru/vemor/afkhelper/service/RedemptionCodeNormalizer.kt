package ru.vemor.afkhelper.service

/**
 * Нормализует код возмещения единообразно на всех путях ввода (добавление и
 * активация): обрезает обрамляющие пробелы. Регистр сохраняется как есть — код
 * активируется ровно в том виде, в котором передан в REST или Telegram.
 */
object RedemptionCodeNormalizer {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            throw InvalidCodeException("Code must not be blank")
        }
        return trimmed
    }
}
