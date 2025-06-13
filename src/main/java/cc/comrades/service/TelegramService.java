package cc.comrades.service;

import cc.comrades.bot.BotClient;
import cc.comrades.model.entity.TelegramSession;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

public class TelegramService {
    public static SendResponse reply(long chatId, String text, boolean forceReply) {
        return reply(chatId, text, null, null, forceReply);
    }

    public static SendResponse reply(long chatId, String text) {
        return reply(chatId, text, null, null, false);
    }

    public static SendResponse reply(long chatId, String text, ParseMode parseMode, boolean forceReply) {
        return reply(chatId, text, null, parseMode, forceReply);
    }

    public static SendResponse reply(long chatId, String text, ParseMode parseMode) {
        return reply(chatId, text, null, parseMode, false);
    }

    public static SendResponse reply(long chatId, String text, Integer threadId, ParseMode parseMode, boolean forceReply) {
        SendMessage message = new SendMessage(chatId, text);
        if (forceReply) {
            message.replyMarkup(new ForceReply());
        }
        if (threadId != null) {
            message.messageThreadId(threadId);
        }
        if (parseMode != null) {
            message.parseMode(parseMode);
        }
        return BotClient.getInstance().getBot().execute(message);
    }

    public static void editMessage(long chatId, int messageId, String newText, ParseMode parseMode) {
        EditMessageText editMessageText = new EditMessageText(chatId, messageId, newText);

        if (parseMode != null) {
            editMessageText.parseMode(parseMode);
        }

        BotClient.getInstance().getBot().execute(editMessageText);
    }

    public static void editMessage(long chatId, int messageId, String newText) {
        editMessage(chatId, messageId, newText, null);
    }

    public static void removeMarkupsAndEdit(CallbackQuery query, long chatId, String newText, ParseMode parseMode) {
        int messageId = query.maybeInaccessibleMessage().messageId();

        editMessage(chatId, messageId, newText, parseMode);
        BotClient.getInstance().getBot().execute(new EditMessageReplyMarkup(chatId, messageId));
        BotClient.getInstance().getBot().execute(new AnswerCallbackQuery(query.id()));
    }

    public static void removeMarkupsAndEdit(CallbackQuery query, long chatId, String newText) {
       removeMarkupsAndEdit(query, chatId, newText, null);
    }
}