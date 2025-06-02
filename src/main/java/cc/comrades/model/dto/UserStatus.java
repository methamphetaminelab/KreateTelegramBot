package cc.comrades.model.dto;

public enum UserStatus {
    WAITING_FOR_NAME,
    WAITING_FOR_HOURS,
    WAITING_FOR_INFO,
    WAITING_FOR_BIO,
    WAITING_FOR_LINKS,
    WAITING_FOR_DISCORD,
    PENDING,
    REJECTED,
    APPROVED;

    public UserStatus next() {
        return ordinal() < values().length - 1 ? values()[ordinal() + 1] : null;
    }
}
