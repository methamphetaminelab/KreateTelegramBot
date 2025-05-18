package cc.comrades.clients;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;

public class SimpleHttpClient {
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Gson gson = new Gson();

    public static <T> T sendGetRequest(String url, Class<T> clazz) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        for (int attempt = 0; attempt < 2; attempt++) {
            try (Response response = httpClient.newCall(request).execute()) {
                int status = response.code();
                if (status == 429) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry delay", e);
                    }
                    continue;
                }
                if (status / 100 == 2) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        return gson.fromJson(body.charStream(), clazz);
                    }

                    return null;
                }
                if (status / 100 == 4) {
                    throw new IOException("Client error: " + status);
                }
                if (status / 100 == 5) {
                    throw new IOException("Server error: " + status);
                }
                throw new IOException("Unsupported status code: " + status);
            }
        }
        throw new IOException("Failed to get a valid response after 2 attempts");
    }
}
