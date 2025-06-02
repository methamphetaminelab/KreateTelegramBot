package cc.comrades.bot.handlers;

import cc.comrades.bot.buttons.ButtonRegistry;
import cc.comrades.clients.RCONClient;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.service.DBService;
import cc.comrades.service.TelegramService;
import cc.comrades.util.StringUtil;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ButtonHandler {

    public static void onUpdate(long chatId, String data, Update update) {
        log.info("Received button press: {}", data);
        ButtonRegistry.get(data.split("\\$")[0]).execute(update.callbackQuery());
    }

    public static void onYesButtonPress(CallbackQuery query) {
        MaybeInaccessibleMessage message = query.maybeInaccessibleMessage();
        long chatId = message.chat().id();
        String mcUsername = query.data().split("\\$")[1];

        log.info("Processing approval for user: {}", mcUsername);

        TelegramSession session = DBService.findSessionByMinecraftUsername(mcUsername);
        if (session != null) {
            session.approveWhitelist();
            DBService.saveSession(session);
            TelegramService.reply(session.getChatId(), "Твоя заявка была одобрена. Добро пожаловать на сервер!");
        } else {
            log.error("Session not found for username: {}", mcUsername);
            TelegramService.reply(chatId, "Произошла неизвестная ошибка.");
            throw new IllegalStateException("Session not found for username: " + mcUsername);
        }

        addToRCON(query, chatId, mcUsername);
    }

    public static void onNoButtonPress(CallbackQuery query) {
        MaybeInaccessibleMessage message = query.maybeInaccessibleMessage();
        long chatId = message.chat().id();
        String mcUsername = query.data().split("\\$")[1];

        log.info("Processing rejection for user: {}", mcUsername);

        TelegramSession session = DBService.findSessionByMinecraftUsername(mcUsername);
        if (session != null) {
            session.rejectWhitelist();
            DBService.saveSession(session);
            TelegramService.reply(session.getChatId(), "Твоя заявка была отклонена :(");
        } else {
            log.error("Session not found for username: {}", mcUsername);
            TelegramService.reply(chatId, "Произошла неизвестная ошибка.");
            throw new IllegalStateException("Session not found for username: " + mcUsername);
        }

        User tgUser = query.from();

        TelegramService.removeAndEdit(query, chatId, "Заявка пользователя " + mcUsername + " была отклонена: " +
                TelegramService.getTelegramMentionString(tgUser.username(), tgUser.id(), tgUser.firstName()));
    }

    private static void addToRCON(CallbackQuery query, long chatId, String mcUsername) {
        User user = query.from();

        try {
            RCONClient.getInstance().sendCommand("simplewhitelist add " + StringUtil.sanitize(mcUsername));
            TelegramService.removeAndEdit(query, chatId, "Пользователь " + mcUsername + " был добавлен в " +
                    "белый список: " +
                    TelegramService.getTelegramMentionString(user.username(), user.id(), user.firstName()));
        } catch (IOException e) {
            log.error("Failed to whitelist user: " + mcUsername, e);
            TelegramService.removeAndEdit(query, chatId, "Не удалось добавить пользователя " + mcUsername +
                    " в белый список");
        }
    }
}
