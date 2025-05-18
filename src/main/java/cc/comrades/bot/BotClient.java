package cc.comrades.bot;

import cc.comrades.bot.handlers.CommandHandler;
import cc.comrades.bot.handlers.MessageHandler;
import cc.comrades.bot.handlers.ButtonHandler;
import cc.comrades.util.EnvLoader;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotClient {
    private static TelegramBot bot;
    private static BotClient instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    private BotClient(String token) {
        bot = new TelegramBot(token);
    }

    public static BotClient getInstance() {
        if (instance == null) {
            instance = new BotClient(EnvLoader.get("BOT_TOKEN"));
        }
        return instance;
    }

    public void start() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                executor.submit(() -> {
                    if (update.message() != null && update.message().text() != null) {
                        String text = update.message().text();
                        long chatId = update.message().chat().id();
                        if (text.startsWith("/")) {
                            CommandHandler.onUpdate(chatId, text, update);
                        } else {
                            MessageHandler.onUpdate(chatId, text, update);
                        }
                    } else if (update.callbackQuery() != null) {
                        CallbackQuery callback = update.callbackQuery();
                        long chatId = callback.maybeInaccessibleMessage().chat().id();
                        String data = callback.data();
                        ButtonHandler.onUpdate(chatId, data, update);
                    }
                });
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public TelegramBot getBot() {
        return bot;
    }
}