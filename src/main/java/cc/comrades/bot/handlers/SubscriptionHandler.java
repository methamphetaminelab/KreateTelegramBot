package cc.comrades.bot.handlers;

import cc.comrades.clients.RCONClient;
import cc.comrades.model.dto.CancelledSubscriptionEvent;
import cc.comrades.model.dto.NewSubscriptionEvent;
import cc.comrades.model.dto.UserStatus;
import cc.comrades.model.entity.TelegramSession;
import cc.comrades.util.DBSessionsManager;
import cc.comrades.util.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubscriptionHandler {
    public static void onSubscription(NewSubscriptionEvent event) {
        NewSubscriptionEvent.Payload payload = event.getPayload();
        if (payload == null) {
            log.error("Received subscription event with null payload: {}", event);
            return;
        }

        long telegramUserId = payload.getTelegramUserId();
        TelegramSession existingSession = DBSessionsManager.findFirstByField(TelegramSession.class,
                "chatId", telegramUserId);


        if (existingSession == null) {
            TelegramSession session = new TelegramSession();
            session.setStatus(UserStatus.WAITING_FOR_NAME);
            session.setChatId(telegramUserId);
            session.setTelegramUsername(payload.getChannelName());
            session.setSubscribed(true);
            DBSessionsManager.saveObject(session);

            sendSubscriptionMessage(telegramUserId);
            return;
        } else if (existingSession.getUsername() == null) {
            existingSession.setSubscribed(true);
            DBSessionsManager.saveObject(existingSession);

            sendSubscriptionMessage(telegramUserId);
            return;
        }

        existingSession.setSubscribed(true);
        DBSessionsManager.saveObject(existingSession);

        try {
            Util.reply(telegramUserId, "Спасибо за подписку! Ваш ник уже указан, привилегии активированы.");
            RCONClient.getInstance().sendCommand(String.format("lp user %s parent add donate", existingSession.getUsername()));
        } catch (Exception e) {
            log.error("Failed to send message to user {}: {}", telegramUserId, e.getMessage());
        }
    }

    public static void onCancelSubscription(CancelledSubscriptionEvent event) {
        CancelledSubscriptionEvent.Payload payload = event.getPayload();
        if (payload == null) {
            log.error("Received cancelled subscription event with null payload: {}", event);
            return;
        }

        long telegramUserId = payload.getTelegramUserId();
        TelegramSession existingSession = DBSessionsManager.findFirstByField(TelegramSession.class,
                "chatId", telegramUserId);


        if (existingSession == null) {
            return;
        } else if (existingSession.getUsername() == null) {
            existingSession.setSubscribed(false);
            DBSessionsManager.saveObject(existingSession);
            return;
        }

        existingSession.setSubscribed(false);
        DBSessionsManager.saveObject(existingSession);

        try {
            Util.reply(telegramUserId, "Вы отписались, привилегии отключены.");
            RCONClient.getInstance().sendCommand(String.format("lp user %s parent remove donate",
                    existingSession.getUsername()));
        } catch (Exception e) {
            log.error("Failed to remove subscription for user {}: {}", existingSession.getUsername(), e.getMessage());
        }
    }

    private static void sendSubscriptionMessage(long chatId) {
        try {
            Util.reply(chatId, "Спасибо за подписку! Укажите свой ник в Minecraft командой " +
                    "/setname your_nickname, " +
                    "чтобы получить привилегии", true);
        }
        catch (Exception e) {
            log.error("Failed to send message to user {}: {}", chatId, e.getMessage());
        }
    }
}
