package cc.comrades.service;

import cc.comrades.model.entity.TelegramSession;
import cc.comrades.model.entity.WhitelistUser;

public class DBService {
    public static TelegramSession findSessionByMinecraftUsername(String username) {
        WhitelistUser user = getOrCreateWhitelistUser(username);
        return DBSessionsManager.findFirstByField(TelegramSession.class, "user", user);
    }

    public static TelegramSession findSessionByChatId(long chatId) {
        return DBSessionsManager.findFirstByField(TelegramSession.class, "chatId", chatId);
    }

    public static TelegramSession saveSession(TelegramSession session) {
        return DBSessionsManager.saveObject(session);
    }

    public static WhitelistUser findWhitelistUserByUsername(String username) {
        return DBSessionsManager.findFirstByField(WhitelistUser.class, "username", username);
    }

    public static WhitelistUser saveWhitelistUser(WhitelistUser user) {
        return DBSessionsManager.saveObject(user);
    }

    public static WhitelistUser getOrCreateWhitelistUser(String username) {
        return getOrCreateWhitelistUser(username, false);
    }

    public static WhitelistUser getOrCreateWhitelistUser(String username, boolean whitelist) {
        return DBSessionsManager.getOrCreate(WhitelistUser.class, "username", username,
                () -> new WhitelistUser(username, MinecraftService.toUUID(MinecraftService.getMinecraftUUID(username)),
                        whitelist)
        );
    }
}
