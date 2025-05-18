package cc.comrades.model.entity;

import cc.comrades.model.dto.UserStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "telegram_users")
@Data
public class TelegramSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column
    private String username;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
}
