package ru.vemor.afkhelper.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import java.sql.SQLException

/**
 * Надёжное определение «duplicate key» нарушения уникальности вместо хрупкого
 * сравнения текста сообщения: проверяет тип Spring, SQLState 23505 и известные
 * формулировки драйвера.
 */
object DuplicateKeyDetector {
    private val duplicateMessage = Regex("(?i)(duplicate key|unique constraint|23505)")

    fun isDuplicateKey(e: DataIntegrityViolationException): Boolean {
        val cause = e.mostSpecificCause
        return e is DuplicateKeyException ||
            (cause as? SQLException)?.sqlState == "23505" ||
            duplicateMessage.containsMatchIn(cause.message ?: "")
    }
}
