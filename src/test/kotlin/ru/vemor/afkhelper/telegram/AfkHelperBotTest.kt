package ru.vemor.afkhelper.telegram

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AfkHelperBotTest {
    private val properties = TelegramBotProperties(token = "token-1")
    private val telegramClient: TelegramClient = Mockito.mock(TelegramClient::class.java)
    private val messageHandler: TelegramMessageHandler = Mockito.mock(TelegramMessageHandler::class.java)
    private val bot = AfkHelperBot(properties, telegramClient, messageHandler)

    @Test
    fun `returns the configured bot token`() {
        assertEquals("token-1", bot.botToken)
    }

    @Test
    fun `processes a text message via handler and sends the reply`() {
        Mockito.doReturn("справка").`when`(messageHandler).handle("/help")
        val sent = captureSentMessages()

        bot.updatesConsumer.consume(listOf(textUpdate(chatId = 123L, text = "/help")))

        assertEquals(1, sent.size)
        assertEquals("123", sent[0].chatId)
        assertEquals("справка", sent[0].text)
        Mockito.verify(messageHandler).handle("/help")
    }

    @Test
    fun `ignores updates without a text message`() {
        val sent = captureSentMessages()

        bot.updatesConsumer.consume(listOf(Update().apply { updateId = 1 }))

        assertTrue(sent.isEmpty())
    }

    @Test
    fun `does not propagate handler failures`() {
        Mockito.doThrow(IllegalStateException("boom")).`when`(messageHandler).handle("/add X")
        val sent = captureSentMessages()

        bot.updatesConsumer.consume(listOf(textUpdate(chatId = 1L, text = "/add X")))

        assertTrue(sent.isEmpty())
    }

    private fun captureSentMessages(): MutableList<SendMessage> {
        val sent = mutableListOf<SendMessage>()
        Mockito
            .doAnswer { invocation ->
                sent += invocation.getArgument<SendMessage>(0)
                null
            }.`when`(telegramClient)
            .execute(Mockito.any<SendMessage>())
        return sent
    }

    private fun textUpdate(
        chatId: Long,
        text: String,
    ): Update {
        val chat =
            Chat
                .builder()
                .id(chatId)
                .type("private")
                .build()
        val msg =
            Message
                .builder()
                .messageId(1)
                .chat(chat)
                .text(text)
                .build()
        return Update().apply {
            updateId = 1
            message = msg
        }
    }
}
