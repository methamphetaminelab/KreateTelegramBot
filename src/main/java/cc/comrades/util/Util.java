package cc.comrades.util;

import cc.comrades.bot.BotClient;
import cc.comrades.clients.SimpleHttpClient;
import cc.comrades.model.dto.MinecraftUser;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import java.util.UUID;

public class Util {
    public static String[] getWhitelistArray(String input) {
        String[] parts = input.split(": ");
        if (parts.length > 1) {
            return parts[1].split(",\\s*");
        }

        return new String[0];
    }

    public static UUID toUUID(String rawUUID) {
        return UUID.fromString(
                rawUUID.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                )
        );
    }

    public static String getMinecraftUUID(String username) {
        username = sanitize(username);

        try {
            MinecraftUser mcUser = SimpleHttpClient.sendGetRequest(
                    "https://api.mojang.com/users/profiles/minecraft/" + username, MinecraftUser.class);

            if (mcUser == null) {
                throw new IllegalArgumentException("User not found: " + username);
            } else {
                return mcUser.getId();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching UUID for user: " + username, e);
        }
    }

    public static boolean doesPlayerExist(String username) {
        try {
            return SimpleHttpClient.sendGetRequest(
                    "https://api.mojang.com/users/profiles/minecraft/" + username, MinecraftUser.class) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }

    public static SendResponse reply(long chatId, String text, boolean forceReply) {
        return reply(chatId, text, null, forceReply);
    }

    public static SendResponse reply(long chatId, String text, Integer threadId) {
        return reply(chatId, text, threadId, false);
    }

    public static SendResponse reply(long chatId, String text, Integer threadId, boolean forceReply) {
        SendMessage message = forceReply ? new SendMessage(chatId, text)
                .replyMarkup(new ForceReply()) : new SendMessage(chatId, text);

        if (forceReply) {
            message.replyMarkup(new ForceReply());
        }

        if (threadId != null) {
            message.messageThreadId(threadId);
        }

        return BotClient.getInstance().getBot().execute(message);
    }

    public static SendResponse reply(long chatId, String text) {
        return reply(chatId, text, false);
    }

    public static void removeAndEdit(CallbackQuery query, long chatId, String newText) {
        int messageId = query.maybeInaccessibleMessage().messageId();

        editMessage(chatId, messageId, newText);
        BotClient.getInstance().getBot().execute(new EditMessageReplyMarkup(chatId, messageId));
        BotClient.getInstance().getBot().execute(new AnswerCallbackQuery(query.id()));
    }


    public static boolean validateUsername(Long chatId, String username) {
        if (username.length() < 3 || username.length() > 32 || !username.matches("[a-zA-Z0-9_]+")) {
            reply(chatId, "Ник должен содержать от 3 до 32 символов и состоять только из букв, цифр и символов подчеркивания.", true);
            return false;
        } else if (!doesPlayerExist(username)) {
            reply(chatId, "Игрок не найден. Пожалуйста, проверьте правильность написания ника и попробуйте снова.", true);
            return false;
        }
        return true;
    }

    public static void editMessage(long chatId, int messageId, String newText) {
        BotClient.getInstance().getBot().execute(new EditMessageText(chatId, messageId, newText));
    }

    public static boolean validateText(Long chatId, String text) {
        if (text.length() < 1 || text.length() > 255) {
            reply(chatId, "Текст должен содержать от 1 до 255 символов.", true);
            return false;
        }
        return true;
    }
}
