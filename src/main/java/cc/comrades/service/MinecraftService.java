package cc.comrades.service;

import cc.comrades.clients.SimpleHttpClient;
import cc.comrades.model.dto.MinecraftUser;
import cc.comrades.util.StringUtil;

import java.io.IOException;
import java.util.UUID;

public class MinecraftService {
    public static String getMinecraftUUID(String username) {
        String sanitized = StringUtil.sanitize(username);
        MinecraftUser mcUser;
        try {
            mcUser = SimpleHttpClient.sendGetRequest(
                    "https://api.mojang.com/users/profiles/minecraft/" + sanitized, MinecraftUser.class);
        } catch (IOException e) {
            throw new RuntimeException("Error fetching UUID for user: " + sanitized, e);
        }
        if (mcUser == null) {
            throw new IllegalArgumentException("User not found: " + sanitized);
        }

        return mcUser.getId();
    }

    public static boolean doesPlayerExist(String username) {
        try {
            return SimpleHttpClient.sendGetRequest(
                    "https://api.mojang.com/users/profiles/minecraft/" +
                            StringUtil.sanitize(username), MinecraftUser.class) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static UUID toUUID(String rawUUID) {
        return UUID.fromString(
                rawUUID.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                )
        );
    }
}
