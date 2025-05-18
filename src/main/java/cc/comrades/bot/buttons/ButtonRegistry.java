package cc.comrades.bot.buttons;

import cc.comrades.bot.handlers.ButtonHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ButtonRegistry {

    private static final Map<String, ButtonFactory> factoryMap = new ConcurrentHashMap<>();

    static {
        new ButtonFactory("Принять", "yesButton", ButtonHandler::onYesButtonPress);
        new ButtonFactory("Отказать", "noButton", ButtonHandler::onNoButtonPress);
    }

    public static void register(ButtonFactory factory) {
        factoryMap.put(factory.getButtonId(), factory);
    }

    public static ButtonFactory get(String name) {
        return factoryMap.get(name);
    }
}
