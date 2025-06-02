package cc.comrades.bot.handlers;

import cc.comrades.clients.RCONClient;
import cc.comrades.model.dto.CancelledSubscriptionEvent;
import cc.comrades.model.dto.NewSubscriptionEvent;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.service.DBService;
import cc.comrades.service.TelegramService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubscriptionHandler {
    public static void onSubscription(NewSubscriptionEvent event) {
        log.info("Received new subscription event: {}", event);
        NewSubscriptionEvent.Payload payload = event.getPayload();
        if (payload == null) {
            log.error("Received subscription event with null payload: {}", event);
            return;
        }

        long telegramUserId = payload.getTelegramUserId();
        TelegramSession session = DBService.findSessionByChatId(telegramUserId);
        boolean shouldNotify = false;

        if (session == null) {
            session = new TelegramSession(telegramUserId, payload.getChannelName());
            shouldNotify = true;
        } else if (session.getUser() == null) {
            shouldNotify = true;
        }

        session.setSubscribed(true);
        DBService.saveSession(session);
        if (shouldNotify) {
            sendSubscriptionMessage(telegramUserId);
            return;
        }

        try {
            RCONClient.getInstance().sendCommand(String.format("lp user %s parent add donate", session.getUser().getUsername()));
            TelegramService.reply(telegramUserId, "Спасибо за подписку! Ваш ник уже указан, привилегии активированы.");
        } catch (Exception e) {
            log.error("Failed to send message to user {}: {}", telegramUserId, e.getMessage());
        }
    }

    public static void onCancelSubscription(CancelledSubscriptionEvent event) {
        log.info("Received cancelled subscription event: {}", event);
        CancelledSubscriptionEvent.Payload payload = event.getPayload();
        if (payload == null) {
            log.error("Received cancelled subscription event with null payload: {}", event);
            return;
        }

        long telegramUserId = payload.getTelegramUserId();
        TelegramSession session = DBService.findSessionByChatId(telegramUserId);

        if (session == null) {
            return;
        }
        session.setSubscribed(false);
        DBService.saveSession(session);

        if (session.getUser() == null) {
            return;
        }

        try {
            TelegramService.reply(telegramUserId, "Вы отписались, привилегии отключены.");
            RCONClient.getInstance().sendCommand(String.format("lp user %s parent remove donate",
                    session.getUser().getUsername()));
        } catch (Exception e) {
            log.error("Failed to remove subscription for user {}: {}", session.getUser().getUsername(), e.getMessage());
        }
    }

    private static void sendSubscriptionMessage(long chatId) {
        try {
            TelegramService.reply(chatId, "Спасибо за подписку! Укажите свой ник в Minecraft командой " +
                    "/setname your_nickname, " +
                    "чтобы получить привилегии", true);
        }
        catch (Exception e) {
            log.error("Failed to send message to user {}: {}", chatId, e.getMessage());
        }
    }
}
