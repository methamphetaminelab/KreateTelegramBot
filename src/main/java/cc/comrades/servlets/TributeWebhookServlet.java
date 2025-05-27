package cc.comrades.servlets;

import cc.comrades.bot.handlers.SubscriptionHandler;
import cc.comrades.model.dto.CancelledSubscriptionEvent;
import cc.comrades.model.dto.NewSubscriptionEvent;
import cc.comrades.util.EnvLoader;
import com.google.gson.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public class TributeWebhookServlet extends HttpServlet {
    private static final String SIGNATURE_HEADER = "trbt-signature";
    private static final String TRIBUTE_API_KEY = EnvLoader.get("TRIBUTE_API_KEY");

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(OffsetDateTime.class,
                    (JsonDeserializer<OffsetDateTime>) (json, type, ctx) -> OffsetDateTime.parse(json.getAsString()))
            .create();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String signature = request.getHeader(SIGNATURE_HEADER);
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!isValidSignature(signature, body)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        JsonElement nameElement = jsonObject.get("name");
        if (nameElement == null || nameElement.isJsonNull()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String name = jsonObject.get("name").getAsString();

        switch (name) {
            case "new_subscription" -> {
                NewSubscriptionEvent event = GSON.fromJson(body, NewSubscriptionEvent.class);
                SubscriptionHandler.onSubscription(event);
            }
            case "cancelled_subscription" -> {
                CancelledSubscriptionEvent event = GSON.fromJson(body, CancelledSubscriptionEvent.class);
                SubscriptionHandler.onCancelSubscription(event);
            }
            case "test_event" -> response.setStatus(HttpServletResponse.SC_OK);
            default -> {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean isValidSignature(String signature, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(TRIBUTE_API_KEY.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac).equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

