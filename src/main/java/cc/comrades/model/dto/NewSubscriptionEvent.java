package cc.comrades.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Data
public class NewSubscriptionEvent {
    private OffsetDateTime createdAt;
    private String name;
    private Payload payload;
    private OffsetDateTime sentAt;

    @Setter
    @Getter
    public static class Payload {
        private String subscriptionName;
        private long subscriptionId;
        private long periodId;
        private String period;
        private long price;
        private long amount;
        private String currency;
        private long userId;
        private long telegramUserId;
        private long channelId;
        private String channelName;
        private OffsetDateTime expiresAt;
    }
}
