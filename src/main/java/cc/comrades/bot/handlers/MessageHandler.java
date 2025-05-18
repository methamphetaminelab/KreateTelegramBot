package cc.comrades.bot.handlers;

import cc.comrades.bot.BotClient;
import cc.comrades.bot.buttons.ButtonRegistry;
import cc.comrades.bot.buttons.Buttons;
import cc.comrades.model.dto.UserStatus;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.util.DBSessionsManager;
import cc.comrades.util.EnvLoader;
import cc.comrades.util.Util;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

public class MessageHandler {
    public static void onUpdate(long chatId, String text, Update update) {
        if (!update.message().chat().type().equals(Chat.Type.Private)) {
            return;
        }

        TelegramSession session = DBSessionsManager.findFirstByField(TelegramSession.class, "chatId", chatId);

        if (session == null) {
            BotClient.getInstance().getBot().execute(new SendMessage(chatId,
                    "Пожалуйста, начните с команды /start, чтобы начать процесс регистрации."));
            return;
        }

        switch (session.getStatus()) {
            case WAITING_FOR_NAME -> {
                if (!Util.validateUsername(chatId, text)) {
                    return;
                }

                session.setUsername(text);
                session.setStatus(UserStatus.WAITING_FOR_HOURS);
                saveSession(session);

                Util.reply(chatId, "Сколько у тебя опыта игры на мультиплеер серверах Майнкрафт (часы)?", true);
            }
            case WAITING_FOR_HOURS -> {
                if (!Util.validateText(chatId, text)) {
                    return;
                }

                session.setHours(text);
                session.setStatus(UserStatus.WAITING_FOR_INFO);
                saveSession(session);

                Util.reply(chatId, "Кто ты и почему ты хочешь попасть к нам на сервер? Расскажи о себе.", true);
            }
            case WAITING_FOR_INFO -> {
                if (!Util.validateText(chatId, text)) {
                    return;
                }

                session.setInfo(text);
                session.setStatus(UserStatus.WAITING_FOR_BIO);
                saveSession(session);

                Util.reply(chatId, "Чем планируешь заняться на сервере?", true);
            }
            case WAITING_FOR_BIO -> {
                if (!Util.validateText(chatId, text)) {
                    return;
                }

                session.setBio(text);
                session.setStatus(UserStatus.WAITING_FOR_LINKS);
                saveSession(session);

                Util.reply(chatId, "Если у тебя есть какие-нибудь работы, поделись ссылкой. Если нет, ответь `-` на это сообщение.", true);
            }
            case WAITING_FOR_LINKS -> {
                if (!Util.validateText(chatId, text)) {
                    return;
                }

                if (!text.trim().equals("-")) {
                    session.setLinks(text);
                }
                session.setStatus(UserStatus.WAITING_FOR_DISCORD);
                saveSession(session);

                Util.reply(chatId, "Напиши свой ник в Дискорд. Если у тебя его нет, ответь `-` на это сообщение.", true);
            }
            case WAITING_FOR_DISCORD -> {
                if (!Util.validateText(chatId, text)) {
                    return;
                }

                if (!text.trim().equals("-")) {
                    session.setDiscord(text);
                }
                session.setStatus(UserStatus.PENDING);
                saveSession(session);

                Util.reply(chatId, "Твоя заявка была принята к рассмотрению. " +
                        "Ты получишь уведомление, когда она будет обработана.", false);
                sendWhitelistRequest(session.getUsername());
            }
        }
    }

    private static void sendWhitelistRequest(String username) {
        TelegramSession session = DBSessionsManager.findFirstByField(TelegramSession.class, "username", username);

        StringBuilder messageBuilder = new StringBuilder("Новая заявка!\n");

        if (session == null) {
            throw new IllegalArgumentException("Session not found for username: " + username);
        }

        if (session.getHours() != null) {
            messageBuilder.append("Опыт игры (часы): ").append(session.getHours()).append("\n");
        }
        if (session.getInfo() != null) {
            messageBuilder.append("Почему должны быть на сервере: ").append(session.getInfo()).append("\n");
        }
        if (session.getBio() != null) {
            messageBuilder.append("Чем займётесь: ").append(session.getBio()).append("\n");
        }
        if (session.getLinks() != null && !session.getLinks().trim().equals("-")) {
            messageBuilder.append("Ссылки: ").append(session.getLinks()).append("\n");
        }
        if (session.getDiscord() != null && !session.getDiscord().trim().equals("-")) {
            messageBuilder.append("Дискорд: ").append(session.getDiscord()).append("\n");
        }

        SendMessage message = new SendMessage(Long.parseLong(EnvLoader.get("TARGET_CHAT_ID")), messageBuilder.toString());

        message.replyMarkup(Buttons.create().row(
                ButtonRegistry.get("yesButton").create(username),
                ButtonRegistry.get("noButton").create(username)
        ).build());

        BotClient.getInstance().getBot().execute(message);
    }

    private static void saveSession(TelegramSession session) {
        DBSessionsManager.saveObject(session);
    }

}
