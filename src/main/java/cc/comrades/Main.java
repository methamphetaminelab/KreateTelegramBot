package cc.comrades;

import cc.comrades.bot.BotClient;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        BotClient.getInstance().start();
    }
}