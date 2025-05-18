package cc.comrades.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> envs = new HashMap<>();
    private static boolean isLoaded = false;

    public static void loadEnvVariables() {
        if (isLoaded) {
            return;
        }

        try {
            for (String line : Files.readAllLines(Paths.get(".env"))) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (!key.isBlank() && !value.isBlank()) {
                        envs.put(key, value);
                    }
                }
            }

            isLoaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Could not load .env file", e);
        }
    }

    public static String get(String key) {
        if (!isLoaded) {
            loadEnvVariables();
        }

        String value = envs.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Environment variable " + key + " not found or empty");
        }
        return value;
    }

    public static Map<String, String> getEnvs() {
        if (!isLoaded) {
            loadEnvVariables();
        }

        return Collections.unmodifiableMap(envs);
    }
}
