package cc.comrades.service;

import cc.comrades.bot.BotClient;
import cc.comrades.model.entity.TelegramSession;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

public class TelegramService {
    public static SendResponse reply(long chatId, String text, boolean forceReply) {
        return reply(chatId, text, null, forceReply);
    }

    public static SendResponse reply(long chatId, String text, Integer threadId) {
        return reply(chatId, text, threadId, false);
    }

    public static SendResponse reply(long chatId, String text) {
        return reply(chatId, text, null, false);
    }

    public static SendResponse reply(long chatId, String text, Integer threadId, boolean forceReply) {
        SendMessage message = new SendMessage(chatId, text);
        if (forceReply) {
            message.replyMarkup(new ForceReply());
        }
        if (threadId != null) {
            message.messageThreadId(threadId);
        }
        return BotClient.getInstance().getBot().execute(message);
    }

    public static void editMessage(long chatId, int messageId, String newText) {
        BotClient.getInstance().getBot().execute(new EditMessageText(chatId, messageId, newText));
    }

    public static void removeAndEdit(CallbackQuery query, long chatId, String newText) {
        int messageId = query.maybeInaccessibleMessage().messageId();

        editMessage(chatId, messageId, newText);
        BotClient.getInstance().getBot().execute(new EditMessageReplyMarkup(chatId, messageId));
        BotClient.getInstance().getBot().execute(new AnswerCallbackQuery(query.id()));
    }

    public static String getTelegramMentionString(TelegramSession session, String text) {
        return getTelegramMentionString(session.getTelegramUsername(), session.getChatId(), text);
    }

    public static String getTelegramMentionString(String telegramUsername, Long chatId, String text) {
        return telegramUsername != null ? "@" + telegramUsername :
                "<a href=\"tg://user?id=" + chatId + "\">" + text + "</a>";
    }
}