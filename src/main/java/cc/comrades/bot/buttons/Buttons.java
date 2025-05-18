package cc.comrades.bot.buttons;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Buttons {
    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    public static Buttons create() {
        return new Buttons();
    }

    public Buttons row(Button... buttons) {
        rows.add(Arrays.stream(buttons)
                .map(Button::create)
                .collect(Collectors.toList()));
        return this;
    }

    public InlineKeyboardMarkup build() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        rows.forEach(row -> markup.addRow(row.toArray(InlineKeyboardButton[]::new)));
        return markup;
    }
}


