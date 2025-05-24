package cc.comrades.bot.handlers;

import cc.comrades.clients.RCONClient;
import cc.comrades.model.dto.UserStatus;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.model.entity.WhitelistUser;
import cc.comrades.util.DBSessionsManager;
import cc.comrades.util.EnvLoader;
import cc.comrades.util.Util;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class CommandHandler {
    public static void onUpdate(long chatId, String command, Update update) {
        String[] args = command.split(" ");
        String username = update.message().from().username();
        switch (args[0].substring(1).toLowerCase()) {
            case "start" -> {
                if (!update.message().chat().type().equals(Chat.Type.Private)) {
                    return;
                }
                handleStartCommand(username, chatId);
            }
            case "update_whitelist" -> handleUpdateWhitelistCommand(chatId);
            case "status" -> {
                if (args.length < 2) {
                    Util.reply(chatId, "Пожалуйста, укажите свой ник в Minecraft, " +
                            "чтобы проверить статус. Например: /status your_nickname");
                    return;
                }
                handleStatusCommand(chatId, args[1]);
            }
            case "setname" -> {
                if (args.length < 2) {
                    Util.reply(chatId, "Пожалуйста, укажите свой ник в Minecraft. " +
                            "Например: /setname your_nickname");
                    return;
                }
                handleSetNameCommand(username, chatId, args);
            }
//            default -> {
//                Util.reply(chatId, "Неизвестная команда. " +
//                        "Пожалуйста, используйте /start для начала оформления заявки.");
//            }
        }
    }

    private static void handleSetNameCommand(String telegramUsername, long chatId, String[] args) {
        String username = args[1];
        if (!Util.validateUsername(chatId, username)) {
            return;
        }

        TelegramSession session = DBSessionsManager.findFirstByField(TelegramSession.class, "chatId", chatId);
        if (session == null) {
            session = new TelegramSession();
            session.setChatId(chatId);
            session.setTelegramUsername(telegramUsername);
            session.setStatus(UserStatus.WAITING_FOR_HOURS);
        }
        session.setUsername(username);
        DBSessionsManager.saveObject(session);

        Util.reply(chatId, "Ник успешно установлен");

        if (session.isSubscribed()) {

        }
    }

    private static void handleStatusCommand(long chatId, String username) {
        Message message = Util.reply(chatId, "Проверяем статус...").message();

        WhitelistUser user = DBSessionsManager.findFirstByField(WhitelistUser.class, "username", username);

        if (user == null) {
            Util.editMessage(chatId, message.messageId(), "Пользователь не найден в базе данных. " +
                    "Пожалуйста, проверьте правильность написания ника и попробуйте снова.");
            return;
        }

        if (user.isWhitelist()) {
            Util.editMessage(chatId, message.messageId(), "Ты в белом списке на сервере!");
        } else {
            Util.editMessage(chatId, message.messageId(), "Ты ещё не в белом списке на сервере. " +
                    "Пожалуйста, дождись одобрения заявки.");
        }
    }

    private static void handleStartCommand(String telegramUsername, long chatId) {
        TelegramSession existingSession = DBSessionsManager.findFirstByField(TelegramSession.class, "chatId", chatId);
        if (existingSession != null) {
            if (existingSession.getStatus() == UserStatus.PENDING) {
                Util.reply(chatId, "У вас уже есть активная заявка. " +
                        "Пожалуйста, дождитесь её обработки.");
                return;
            } else if (existingSession.getStatus() == UserStatus.APPROVED) {
                Util.reply(chatId, "Вы уже одобрены на сервере.");
                return;
            } else if (existingSession.getStatus() == UserStatus.REJECTED) {
                Util.reply(chatId, "Ваша заявка была отклонена.");
                return;
            }
        }

        Util.reply(chatId, "Привет!\n" +
                "Чтобы начать оформление заявки на Креатé, укажите свой ник в Minecraft в ответе на это сообщение.", true);

        if (existingSession != null) {
            DBSessionsManager.deleteObject(existingSession.getClass(), existingSession.getId());
        }

        TelegramSession session = new TelegramSession();
        session.setChatId(chatId);
        session.setTelegramUsername(telegramUsername);
        session.setStatus(UserStatus.WAITING_FOR_NAME);
        DBSessionsManager.saveObject(session);
    }

    private static void handleUpdateWhitelistCommand(long chatId) {
        long targetChatId = Long.parseLong(EnvLoader.get("TARGET_CHAT_ID"));
        if (chatId != targetChatId) {
            return;
        }

        SendResponse message = Util.reply(chatId, "Updating whitelist...");
        updateFromWhitelist();
        Util.editMessage(chatId, message.message().messageId(), "Whitelist updated!");
    }

    public static void updateFromWhitelist() {
        RCONClient client = RCONClient.getInstance();
        try {
            String result = client.sendCommand("whitelist list");
            for (String user : Util.getWhitelistArray(result)) {
                try {
                    String rawUuid = Util.getMinecraftUUID(user);
                    if (DBSessionsManager.findFirstByField(WhitelistUser.class, "uuid", Util.toUUID(rawUuid)) != null) {
                        log.warn("User already exists in the database: " + user + ", skipping");
                        continue;
                    }
                    DBSessionsManager.saveObject(new WhitelistUser(user, Util.toUUID(rawUuid), true));
                } catch (IllegalArgumentException e) {
                    log.warn("User was not found: " + user + ", skipping");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
