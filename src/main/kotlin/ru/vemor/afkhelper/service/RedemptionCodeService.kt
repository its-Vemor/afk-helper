package ru.vemor.afkhelper.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.vemor.afkhelper.domain.RedemptionCode
import ru.vemor.afkhelper.dto.CreateRedemptionCodeRequest
import ru.vemor.afkhelper.dto.RedemptionCodeResponse
import ru.vemor.afkhelper.repository.RedemptionCodeRepository

@Service
class RedemptionCodeService(
    private val repository: RedemptionCodeRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun addCode(request: CreateRedemptionCodeRequest): RedemptionCodeResponse {
        val normalized = RedemptionCodeNormalizer.normalize(request.code)

        if (repository.existsByCode(normalized)) {
            throw CodeAlreadyExistsException("Redemption code '$normalized' already exists")
        }

        val saved = saveRedemptionCode(normalized)
        // Публикуется событие; фоновое автоприменение запускается AFTER_COMMIT-слушателем.
        eventPublisher.publishEvent(RedemptionCodeAddedEvent(saved))
        return saved.toResponse()
    }

    private fun RedemptionCode.toResponse(): RedemptionCodeResponse =
        RedemptionCodeResponse(
            id = requireNotNull(id),
            code = code,
            isActive = isActive,
        )

    private fun saveRedemptionCode(normalized: String): RedemptionCode =
        try {
            repository.save(RedemptionCode(code = normalized))
        } catch (e: DataIntegrityViolationException) {
            if (DuplicateKeyDetector.isDuplicateKey(e)) {
                // Два параллельных запроса с одинаковым кодом: гонка между existsByCode и save.
                throw CodeAlreadyExistsException("Redemption code '$normalized' already exists")
            }
            throw e
        }
}
