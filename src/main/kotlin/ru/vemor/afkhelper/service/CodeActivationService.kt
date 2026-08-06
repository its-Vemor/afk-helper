package ru.vemor.afkhelper.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import ru.vemor.afkhelper.client.LilithApi
import ru.vemor.afkhelper.client.LilithApiException
import ru.vemor.afkhelper.client.RoleInfo
import ru.vemor.afkhelper.domain.Account
import ru.vemor.afkhelper.domain.GameCharacter
import ru.vemor.afkhelper.dto.ActivateCodeRequest
import ru.vemor.afkhelper.dto.CodeActivationResponse
import ru.vemor.afkhelper.repository.RedemptionCodeRepository

@Service
class CodeActivationService(
    private val lilithClient: LilithApi,
    private val accountService: AccountService,
    private val characterSyncService: CharacterSyncService,
    private val redemptionCodeRepository: RedemptionCodeRepository,
) {
    private val log = LoggerFactory.getLogger(CodeActivationService::class.java)

    @Transactional
    fun activate(request: ActivateCodeRequest): CodeActivationResponse =
        doActivate(request.uid, request.redemptionCode, request.authCode)

    @Transactional
    fun activate(
        accountUid: String,
        redemptionCode: String,
    ): CodeActivationResponse = doActivate(accountUid, redemptionCode, null)

    private fun doActivate(
        uid: String,
        redemptionCode: String,
        authCode: String?,
    ): CodeActivationResponse {
        val normalizedCode = RedemptionCodeNormalizer.normalize(redemptionCode)
        requireActiveCode(normalizedCode)

        val account = accountService.ensureAccount(uid, authCode)
        // Защита от null-ответа апстрима: персонажи без ролей считаются пустым списком.
        val roles: List<RoleInfo>? = lilithClient.getRoles(account.uid, account.authToken)
        val characters = characterSyncService.syncCharacters(requireNotNull(account.id), roles.orEmpty())
        val results = activateCharacters(account, characters, normalizedCode)

        return CodeActivationResponse(uid = account.uid, results = results)
    }

    private fun requireActiveCode(normalizedCode: String) {
        val stored =
            redemptionCodeRepository.findByCode(normalizedCode)
                ?: throw InvalidCodeException("Redemption code '$normalizedCode' does not exist")
        if (!stored.isActive) {
            throw InvalidCodeException("Redemption code '$normalizedCode' is not active")
        }
    }

    private fun activateCharacters(
        account: Account,
        characters: List<GameCharacter>,
        redemptionCode: String,
    ): List<CodeActivationResponse.Result> =
        runBlocking {
            characters
                .map { async(Dispatchers.IO) { activateCharacter(account, it, redemptionCode) } }
                .awaitAll()
        }

    private fun activateCharacter(
        account: Account,
        character: GameCharacter,
        redemptionCode: String,
    ): CodeActivationResponse.Result =
        try {
            val exchanged = lilithClient.consume(character.uid, account.authToken, redemptionCode)
            CodeActivationResponse.Result(
                uid = character.uid,
                name = character.name,
                success = exchanged,
                message = if (exchanged) "ok" else "Redemption code was not exchanged",
            )
        } catch (e: LilithApiException) {
            log.debug("Lilith rejected consume for character {}: {}", character.uid, e.message)
            CodeActivationResponse.Result(
                uid = character.uid,
                name = character.name,
                success = false,
                message = e.message,
            )
        } catch (e: RestClientException) {
            log.warn("Transport error while consuming code for character {}", character.uid, e)
            CodeActivationResponse.Result(
                uid = character.uid,
                name = character.name,
                success = false,
                message = "Redemption service unavailable",
            )
        }
}
