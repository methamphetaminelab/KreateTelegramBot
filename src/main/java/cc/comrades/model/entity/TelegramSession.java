package cc.comrades.model.entity;

import cc.comrades.model.dto.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_users")
@Data
@NoArgsConstructor
public class TelegramSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "telegram_username", unique = true)
    private String telegramUsername;

    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinColumn(name = "whitelist_user_id")
    private WhitelistUser user;

    @Column
    private String bio;

    @Column
    private String hours;

    @Column
    private String info;

    @Column
    private String links;

    @Column
    private String discord;

    @Column(name = "is_subscribed", nullable = false)
    private boolean isSubscribed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.WAITING_FOR_NAME;

    public void approveWhitelist() {
        if (this.user == null) {
            throw new IllegalStateException("Cannot approve whitelist: user is null.");
        }

        user.setWhitelist(true);
        this.status = UserStatus.APPROVED;
    }

    public void rejectWhitelist() {
        if (this.user == null) {
            throw new IllegalStateException("Cannot reject whitelist: user is null.");
        }

        user.setWhitelist(false);
        this.status = UserStatus.REJECTED;
    }

    public void updateStatus() {
        this.status = this.status.next();
    }

    public TelegramSession(long chatId, String telegramUsername) {
        this.chatId = chatId;
        this.telegramUsername = telegramUsername;
    }
}
