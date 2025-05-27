package cc.comrades.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class WhitelistUser {

    public WhitelistUser(String username, UUID uuid, boolean whitelist) {
        this.username = username;
        this.uuid = uuid;
        this.whitelist = whitelist;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private boolean whitelist = false;
}