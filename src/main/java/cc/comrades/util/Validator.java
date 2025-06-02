package cc.comrades.util;

import cc.comrades.service.MinecraftService;
import cc.comrades.service.TelegramService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Validator {
    public static boolean validateUsername(Long chatId, String username) {
        if (username.length() < 3 || username.length() > 32 || !username.matches("[a-zA-Z0-9_]+")) {
            TelegramService.reply(chatId,
                    "Ник должен содержать от 3 до 32 символов и состоять только из " +
                            "букв, цифр и символов подчеркивания.", true);
            log.warn("Invalid username format: {} for chatId: {}", username, chatId);
            return false;
        }
        if (!MinecraftService.doesPlayerExist(username)) {
            TelegramService.reply(chatId, "Игрок не найден. Проверь, правильно ли указан ник " +
                    "Minecraft и есть ли у тебя лицензия, а затем попробуй снова.", true);
            log.warn("Player does not exist: {} for chatId: {}", username, chatId);
            return false;
        }
        return true;
    }

    public static boolean validateText(Long chatId, String text) {
        if (text.length() < 1 || text.length() > 255) {
            TelegramService.reply(chatId, "Текст должен содержать от 1 до 255 символов.", true);
            return false;
        }
        return true;
    }
}
