package cc.comrades.service;

import cc.comrades.clients.SimpleHttpClient;
import cc.comrades.model.dto.MinecraftUser;
import cc.comrades.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class MinecraftService {
    public static String getMinecraftUUID(String username) {
        String sanitized = StringUtil.sanitize(username);
        MinecraftUser mcUser;
//        try {
//            mcUser = SimpleHttpClient.sendGetRequest(
//                    "https://api.mojang.com/users/profiles/minecraft/" + sanitized, MinecraftUser.class);

            // Mojang API sucks now, so we return null
            return null;
//        } catch (IOException e) {
//            throw new RuntimeException("Error fetching UUID for user: " + sanitized, e);
//        }
//        if (mcUser == null) {
//            throw new IllegalArgumentException("User not found: " + sanitized);
//        }

//        return mcUser.getId();
    }

    public static boolean doesPlayerExist(String username) {
        try {
            // Well, Mojang API is broken now and returns 403 for any request
            // Maybe use a third-party API in the future?

//            return SimpleHttpClient.sendGetRequest(
//                    "https://api.mojang.com/users/profiles/minecraft/" +
//                            StringUtil.sanitize(username), MinecraftUser.class) != null;
            log.warn("Mojang API is broken, returning true for player existence check: {}", username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static UUID toUUID(String rawUUID) {
        return null;
//        return UUID.fromString(
//                rawUUID.replaceFirst(
//                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
//                        "$1-$2-$3-$4-$5"
//                )
//        );
    }
}
