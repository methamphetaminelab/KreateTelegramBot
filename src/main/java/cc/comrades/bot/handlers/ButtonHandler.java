package cc.comrades.bot.handlers;

import cc.comrades.bot.buttons.ButtonRegistry;
import cc.comrades.clients.RCONClient;
import cc.comrades.model.dto.UserStatus;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.model.entity.WhitelistUser;
import cc.comrades.util.DBSessionsManager;
import cc.comrades.util.Util;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ButtonHandler {

    public static void onUpdate(long chatId, String data, Update update) {
        ButtonRegistry.get(data.split("\\$")[0]).execute(update.callbackQuery());
    }

    public static void onYesButtonPress(CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        String username = query.data().split("\\$")[1];

        WhitelistUser user = DBSessionsManager.findFirstByField(WhitelistUser.class, "username", username);
        if (user == null) {
            user = new WhitelistUser(username, Util.toUUID(Util.getMinecraftUUID(username)), true);
        }
        user.setWhitelist(true);

        DBSessionsManager.saveObject(user);
        TelegramSession session = DBSessionsManager.findFirstByField(TelegramSession.class, "username", username);
        if (session != null) {
            session.setStatus(UserStatus.APPROVED);
            DBSessionsManager.saveObject(session);
            Util.reply(session.getChatId(), "Твоя заявка была одобрена. Добро пожаловать на сервер!");
        }

        addToRCON(query, chatId, username);
    }

    public static void onNoButtonPress(CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        String[] args = query.data().split("\\$");
        if (args.length < 2) {
            return;
        }

        String username = args[1];

        WhitelistUser user = DBSessionsManager.findFirstByField(WhitelistUser.class, "username", username);
        if (user == null) {
            user = new WhitelistUser(username, Util.toUUID(Util.getMinecraftUUID(username)), false);
        }
        user.setWhitelist(false);

        DBSessionsManager.saveObject(user);
        TelegramSession session = DBSessionsManager.findFirstByField(TelegramSession.class, "username", username);
        if (session != null) {
            session.setStatus(UserStatus.REJECTED);
            DBSessionsManager.saveObject(session);
            Util.reply(session.getChatId(), "Твоя заявка была отклонена :(");
        }

        Util.removeAndEdit(query, chatId, "Заявка пользователя " + username + " была отклонена");
    }

    private static void addToRCON(CallbackQuery query, long chatId, String username) {
        try {
            RCONClient.getInstance().sendCommand("whitelist add " + Util.sanitize(username));
            Util.removeAndEdit(query, chatId, "Пользователь " + username + " был добавлен в белый список");
        } catch (IOException e) {
            log.error("Failed to whitelist user: " + username, e);
            Util.removeAndEdit(query, chatId, "Не удалось добавить пользователя " + username + " в белый список");
        }
    }
}
