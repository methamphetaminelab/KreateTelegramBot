package cc.comrades.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        envs.putAll(System.getenv());

        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
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
            } catch (IOException e) {
                throw new RuntimeException("Could not load .env file", e);
            }
        }
        isLoaded = true;
    }

    public static String get(String key) {
        if (!isLoaded) {
            loadEnvVariables();
        }

        return envs.get(key);
    }

    public static Map<String, String> getEnvs() {
        if (!isLoaded) {
            loadEnvVariables();
        }

        return Collections.unmodifiableMap(envs);
    }
}
