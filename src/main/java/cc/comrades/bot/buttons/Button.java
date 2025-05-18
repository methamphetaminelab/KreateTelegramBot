package cc.comrades.bot.buttons;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import lombok.Data;

@Data
public class Button {
    private String text;
    private String callbackData;

    public Button(String text, String callbackData) {
        this.text = text;
        this.callbackData = callbackData;
    }

    public InlineKeyboardButton create() {
        return new InlineKeyboardButton(text).callbackData(callbackData);
    }
}
