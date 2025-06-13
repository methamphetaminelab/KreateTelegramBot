package cc.comrades.bot.handlers;

import cc.comrades.bot.BotClient;
import cc.comrades.bot.buttons.ButtonRegistry;
import cc.comrades.bot.buttons.Buttons;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.model.entity.WhitelistUser;
import cc.comrades.service.DBService;
import cc.comrades.service.TelegramService;
import cc.comrades.service.DBSessionsManager;
import cc.comrades.util.EnvLoader;
import cc.comrades.util.Markdown;
import cc.comrades.util.Validator;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                if (!Validator.validateUsername(chatId, text)) {
                    return;
                }
                log.info("Received username '{}' for chatId {}", text, chatId);

                // TODO: two requests to Mojang API, cache later
                WhitelistUser user = DBService.getOrCreateWhitelistUser(text, false);

                if (user == null) {
                    log.warn("Failed to create or retrieve WhitelistUser for username: {}", text);
                    TelegramService.reply(chatId, "Произошла неизвестная ошибка. Пожалуйста, попробуй ещё раз.", true);
                    throw new IllegalStateException("Something went terribly wrong.");
                }

                session.setUser(user);
                session.updateStatus();
                DBService.saveSession(session);

                TelegramService.reply(chatId, "Сколько у тебя опыта игры на мультиплеер серверах Майнкрафт (часы)?", true);
            }
            case WAITING_FOR_HOURS -> {
                if (!Validator.validateText(chatId, text)) {
                    return;
                }

                log.info("Received hours '{}' for chatId {}", text, chatId);

                session.setHours(text);
                session.updateStatus();
                DBService.saveSession(session);

                TelegramService.reply(chatId, "Кто ты и почему ты хочешь попасть к нам на сервер? Расскажи о себе.", true);
            }
            case WAITING_FOR_INFO -> {
                if (!Validator.validateText(chatId, text)) {
                    return;
                }

                log.info("Received info '{}' for chatId {}", text, chatId);

                session.setInfo(text);
                session.updateStatus();
                DBService.saveSession(session);

                TelegramService.reply(chatId, "Чем планируешь заняться на сервере?", true);
            }
            case WAITING_FOR_BIO -> {
                if (!Validator.validateText(chatId, text)) {
                    return;
                }

                log.info("Received bio '{}' for chatId {}", text, chatId);

                session.setBio(text);
                session.updateStatus();
                DBService.saveSession(session);

                TelegramService.reply(chatId, "Если у тебя есть какие-нибудь работы, поделись ссылкой. Если нет, ответь `-` на это сообщение.", true);
            }
            case WAITING_FOR_LINKS -> {
                if (!Validator.validateText(chatId, text)) {
                    return;
                }

                log.info("Received links '{}' for chatId {}", text, chatId);

                if (!text.trim().equals("-")) {
                    session.setLinks(text);
                }
                session.updateStatus();
                DBService.saveSession(session);

                TelegramService.reply(chatId, "Напиши свой ник в Дискорд. Если у тебя его нет, ответь `-` на это сообщение.", true);
            }
            case WAITING_FOR_DISCORD -> {
                if (!Validator.validateText(chatId, text)) {
                    return;
                }

                log.info("Received discord '{}' for chatId {}", text, chatId);

                if (!text.trim().equals("-")) {
                    session.setDiscord(text);
                }

                session.updateStatus();
                session = DBService.saveSession(session);

                TelegramService.reply(chatId, "Твоя заявка была принята к рассмотрению. " +
                        "Ты получишь уведомление, когда она будет обработана.", false);
                sendWhitelistRequest(session, update.message().chat().firstName());
            }
        }
    }

    private static void sendWhitelistRequest(TelegramSession session, String firstName) {
        if (session == null) {
            log.warn("Attempted to send whitelist request for null session");
            return;
        }

        WhitelistUser user = session.getUser();
        if (user == null) {
            log.warn("Attempted to send whitelist request for session without user: {}", session);
            return;
        }

        String mcUsername = user.getUsername();

        SendMessage message = new SendMessage(Long.parseLong(EnvLoader.get("TARGET_CHAT_ID")),
                buildUserInfoMessage(session, firstName));

        message.parseMode(ParseMode.MarkdownV2);

        message.replyMarkup(Buttons.create().row(
                ButtonRegistry.get("yesButton").create(mcUsername),
                ButtonRegistry.get("noButton").create(mcUsername)
        ).build());

        String threadId = EnvLoader.get("THREAD_ID");
        if (threadId != null) {
            message.messageThreadId(Integer.parseInt(EnvLoader.get("THREAD_ID")));
        }

        log.info("Sending whitelist request for user: {}", mcUsername);
        BotClient.getInstance().getBot().execute(message);
    }

    public static String buildUserInfoMessage(TelegramSession session, String firstName) {
        Markdown markdown = Markdown.create("Новая заявка!\n");
        StringBuilder messageBuilder = new StringBuilder("Новая заявка!\n");

        if (session.getHours() != null) {
            markdown.text("Опыт игры (часы): ").text(session.getHours()).newLine();
        }
        if (session.getInfo() != null) {
            markdown.text("Почему должны быть на сервере: ").text(session.getInfo()).newLine();
        }
        if (session.getBio() != null) {
            markdown.text("Чем займётесь: ").text(session.getBio()).newLine();
        }
        if (session.getLinks() != null && !session.getLinks().trim().equals("-")) {
            markdown.text("Ссылки: ").text(session.getLinks()).newLine();
        }
        if (session.getDiscord() != null && !session.getDiscord().trim().equals("-")) {
            markdown.text("Дискорд: ").text(session.getDiscord()).newLine();
        }

        markdown.text("Телеграм: ").mention(session, firstName).newLine();

        return markdown.build();
    }
}
