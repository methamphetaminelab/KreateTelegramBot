package cc.comrades.service;

import cc.comrades.clients.SimpleHttpClient;
import cc.comrades.model.dto.MinecraftUser;
import cc.comrades.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class MinecraftService {

    private static final String MOJANG_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MINECRAFT_SERVICES_ENDPOINT = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";

    public static String getMinecraftUUID(String username) {
        String sanitized = StringUtil.sanitize(username);
        try {
            MinecraftUser mcUser = SimpleHttpClient.sendGetRequest(
                    MOJANG_ENDPOINT + sanitized, MinecraftUser.class);

            if (mcUser != null && mcUser.getId() != null) {
                log.info("UUID for {} fetched via Mojang API: {}", sanitized, mcUser.getId());
                return mcUser.getId();
            }

            log.warn("Mojang API returned empty for {}, trying fallback...", sanitized);
            MinecraftUser fallback = SimpleHttpClient.sendGetRequest(
                    MINECRAFT_SERVICES_ENDPOINT + sanitized, MinecraftUser.class);
            if (fallback != null && fallback.getId() != null) {
                log.info("UUID for {} fetched via Minecraft Services fallback: {}", sanitized, fallback.getId());
                return fallback.getId();
            }

            throw new IllegalArgumentException("User not found via both APIs: " + sanitized);

        } catch (IOException e) {
            throw new RuntimeException("Error fetching UUID for user: " + sanitized, e);
        }
    }

    public static boolean doesPlayerExist(String username) {
        String sanitized = StringUtil.sanitize(username);
        try {
            MinecraftUser mcUser = SimpleHttpClient.sendGetRequest(
                    MOJANG_ENDPOINT + sanitized, MinecraftUser.class);

            if (mcUser != null && mcUser.getId() != null) {
                return true;
            }

            MinecraftUser fallback = SimpleHttpClient.sendGetRequest(
                    MINECRAFT_SERVICES_ENDPOINT + sanitized, MinecraftUser.class);
            return fallback != null && fallback.getId() != null;

        } catch (Exception e) {
            log.warn("Error checking existence for {}: {}", sanitized, e.getMessage());
            return false;
        }
    }

    public static UUID toUUID(String rawUUID) {
        String sanitized = rawUUID.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(sanitized);
    }
}
