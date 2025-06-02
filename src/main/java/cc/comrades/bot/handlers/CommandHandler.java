package cc.comrades.bot.handlers;

import cc.comrades.clients.RCONClient;
import cc.comrades.model.dto.UserStatus;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.model.entity.WhitelistUser;
import cc.comrades.service.DBService;
import cc.comrades.service.MinecraftService;
import cc.comrades.service.TelegramService;
import cc.comrades.service.DBSessionsManager;
import cc.comrades.util.EnvLoader;
import cc.comrades.util.StringUtil;
import cc.comrades.util.Validator;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

@Slf4j
public class CommandHandler {
    public static void onUpdate(long chatId, String command, Update update) {
        String[] args = command.split(" ");
        log.info("Received command: {} from user: {}", command, chatId);
        String telegramUsername = update.message().from().username();

        switch (args[0].substring(1).toLowerCase()) {
            case "start" -> {
                if (!update.message().chat().type().equals(Chat.Type.Private)) {
                    return;
                }
                handleStartCommand(telegramUsername, chatId);
            }
            case "update_whitelist" -> handleUpdateWhitelistCommand(chatId);
            case "status" -> {
                if (args.length < 2) {
                    TelegramService.reply(chatId, "Пожалуйста, укажите свой ник в Minecraft, " +
                            "чтобы проверить статус. Например: /status your_nickname");
                    return;
                }
                handleStatusCommand(chatId, args[1]);
            }
            case "setname" -> {
                if (args.length < 2) {
                    TelegramService.reply(chatId, "Пожалуйста, укажите свой ник в Minecraft. " +
                            "Например: /setname your_nickname");
                    return;
                }
                handleSetNameCommand(telegramUsername, chatId, args);
            }
            default -> {
                if (!update.message().chat().type().equals(Chat.Type.Private)) {
                    return;
                }

                TelegramService.reply(chatId, "Неизвестная команда. " +
                        "Пожалуйста, используйте /start для начала оформления заявки.");
            }
        }
    }

    private static void handleSetNameCommand(String telegramUsername, long chatId, String[] args) {
        String username = args[1];
        if (!Validator.validateUsername(chatId, username)) {
            return;
        }

        TelegramSession session = DBService.findSessionByChatId(chatId);
        if (session == null) {
            session = new TelegramSession(chatId, telegramUsername);
            session.setStatus(UserStatus.WAITING_FOR_HOURS);
        }

        session.setUser(DBService.getOrCreateWhitelistUser(username, false));
        session = DBSessionsManager.saveObject(session);

        TelegramService.reply(chatId, "Ник успешно установлен");

        if (session.isSubscribed()) {
            try {
                RCONClient.getInstance().sendCommand(String.format("lp user %s parent add donate",
                        session.getUser().getUsername()));
                TelegramService.reply(chatId, "Спасибо за подписку! Привилегии активированы.");
            } catch (IOException e) {
                log.error("Failed to send message to user {}: {}", chatId, e.getMessage());
            }
        }
    }

    private static void handleStatusCommand(long chatId, String username) {
        Message message = TelegramService.reply(chatId, "Проверяем статус...").message();

        WhitelistUser user = DBService.findWhitelistUserByUsername(username);

        boolean flag = false;

        if (user == null) {
            if (isWhitelistUser(username, e -> TelegramService.editMessage(chatId, message.messageId(),
                    "Не удалось проверить статус"))) {
                flag = true;
            } else {
                TelegramService.editMessage(chatId, message.messageId(), "Ты ещё не в белом списке на сервере.");
                return;
            }
        }

        if (flag || user.isWhitelist()) {
            TelegramService.editMessage(chatId, message.messageId(), "Ты в белом списке на сервере!");
        } else {
            TelegramService.editMessage(chatId, message.messageId(), "Ты ещё не в белом списке на сервере. " +
                    "Пожалуйста, дождись одобрения заявки.");
        }
    }

    private static boolean isWhitelistUser(String username, Consumer<Exception> onException) {
        try {
            String[] usernames = StringUtil.getWhitelistArray(RCONClient.getInstance().sendCommand("simplewhitelist list"));
            return Arrays.asList(usernames).contains(username);
        } catch (IOException e) {
            log.error("Failed to get whitelist list: {}", e.getMessage());
            onException.accept(e);
            throw new RuntimeException(e);
        }
    }

    private static void handleStartCommand(String telegramUsername, long chatId) {
        TelegramSession existingSession = DBService.findSessionByChatId(chatId);
        if (existingSession != null) {
            if (existingSession.getStatus() == UserStatus.PENDING) {
                TelegramService.reply(chatId, "У вас уже есть активная заявка. " +
                        "Пожалуйста, дождитесь её обработки.");
                return;
            } else if (existingSession.getStatus() == UserStatus.APPROVED) {
                TelegramService.reply(chatId, "Вы уже одобрены на сервере.");
                return;
            } else if (existingSession.getStatus() == UserStatus.REJECTED) {
                TelegramService.reply(chatId, "Ваша заявка была отклонена.");
                return;
            }

            DBSessionsManager.deleteObject(existingSession.getClass(), existingSession.getId());
        }

        TelegramService.reply(chatId, "Привет!\n" +
                "Чтобы начать оформление заявки на Креатé, укажите свой ник в Minecraft в ответе на это сообщение " +
                "(лицензия обязательна).", true);

        DBService.saveSession(new TelegramSession(chatId, telegramUsername));
    }

    private static void handleUpdateWhitelistCommand(long chatId) {
        long targetChatId = Long.parseLong(EnvLoader.get("TARGET_CHAT_ID"));
        if (chatId != targetChatId) {
            return;
        }

        SendResponse message = TelegramService.reply(chatId, "Updating whitelist...");
        updateFromWhitelist();
        TelegramService.editMessage(chatId, message.message().messageId(), "Whitelist updated!");
    }

    public static void updateFromWhitelist() {
        try {
            String[] whitelist = StringUtil.getWhitelistArray(RCONClient.getInstance().sendCommand("simplewhitelist list"));
            for (String user : whitelist) {
                try {
                    if (DBService.findWhitelistUserByUsername(user) != null) {
                        log.info("User already exists in the database: " + user + ", skipping");
                        continue;
                    }

                    log.info("Adding user to the database: " + user);
                    DBService.saveWhitelistUser(new WhitelistUser(user, MinecraftService.toUUID(MinecraftService.getMinecraftUUID(user)), true));
                } catch (IllegalArgumentException e) {
                    log.warn("User was not found: " + user + ", skipping");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
