package cc.comrades.bot.buttons;

import com.pengrad.telegrambot.model.CallbackQuery;
import lombok.Data;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class ButtonFactory {
    private String name;
    private String buttonId;
    private Consumer<CallbackQuery> onClick;

    public ButtonFactory(String name, String buttonId, Consumer<CallbackQuery> onClick) {
        this.name = name;
        this.buttonId = buttonId;
        this.onClick = onClick;
        ButtonRegistry.register(this);
    }

    public Button create(String... args) {
        String callback = Stream.concat(Stream.of(buttonId), Arrays.stream(args))
                .collect(Collectors.joining("$"));

        return new Button(name, callback);
    }

    public void execute(CallbackQuery query) {
        onClick.accept(query);
    }
}
