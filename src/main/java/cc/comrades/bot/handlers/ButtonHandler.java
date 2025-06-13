package cc.comrades.bot.handlers;

import cc.comrades.bot.buttons.ButtonRegistry;
import cc.comrades.clients.RCONClient;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.service.DBService;
import cc.comrades.service.TelegramService;
import cc.comrades.util.Markdown;
import cc.comrades.util.StringUtil;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.ParseMode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Consumer;

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
            TelegramService.reply(session.getChatId(),
                    Markdown.create()
                            .text("Твоя заявка была одобрена. Добро пожаловать на сервер!").newLine()
                            .text("Также ты можешь присоединиться к нашей группе в Телеграм: ")
                            .link("Kreate", "https://t.me/+lNxrho0LcqszMDIy").build(),
                    ParseMode.MarkdownV2);
        } else {
            log.error("Session not found for username: {}", mcUsername);
            TelegramService.reply(chatId, "Произошла неизвестная ошибка.");
            throw new IllegalStateException("Session not found for username: " + mcUsername);
        }

        User user = query.from();
        addToRCON(mcUsername, (e) -> TelegramService.removeMarkupsAndEdit(query, chatId,
                Markdown.create("Не удалось добавить пользователя " + mcUsername + " в белый список").build(), ParseMode.MarkdownV2));

        Markdown markdown = Markdown.create();

        markdown.text("Пользователь " + mcUsername + " был добавлен в белый список: ")
                .mention(user.username(), user.firstName(), user.id());

        if (message instanceof Message realMessage) {
            markdown.newLine().newLine().spoiler(realMessage.text());
        }

        TelegramService.removeMarkupsAndEdit(query, chatId, markdown.build(), ParseMode.MarkdownV2);
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
            TelegramService.reply(session.getChatId(), Markdown.create("Твоя заявка была отклонена :(").build(), ParseMode.MarkdownV2);
        } else {
            log.error("Session not found for username: {}", mcUsername);
            TelegramService.reply(chatId, "Произошла неизвестная ошибка.");
            throw new IllegalStateException("Session not found for username: " + mcUsername);
        }

        User user = query.from();

        Markdown markdown = Markdown.create();

        markdown.text("Заявка пользователя " + mcUsername + " была отклонена: ")
                .mention(user.username(), user.firstName(), user.id());

        if (message instanceof Message realMessage) {
            markdown.newLine().newLine().spoiler(realMessage.text());
        }

        TelegramService.removeMarkupsAndEdit(query, chatId, markdown.build(), ParseMode.MarkdownV2);
    }

    private static void addToRCON(String mcUsername, Consumer<Exception> onError) {

        try {
            RCONClient.getInstance().sendCommand("simplewhitelist add " + StringUtil.sanitize(mcUsername));
        } catch (IOException e) {
            log.error("Failed to whitelist user: {}", mcUsername, e);
            onError.accept(e);
        }
    }
}
