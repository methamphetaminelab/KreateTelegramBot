package cc.comrades;

import cc.comrades.bot.BotClient;
import cc.comrades.servlets.TributeWebhookServlet;
import cc.comrades.util.EnvLoader;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

public class Main {
    public static void main(String[] args) {
        new Thread(() -> {
            try {
                startTomcat();
            } catch (LifecycleException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> BotClient.getInstance().start()).start();
    }

    public static void startTomcat() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.parseInt(EnvLoader.get("SERVER_PORT")));
        tomcat.getConnector();
        Context context = tomcat.addContext("", null);
        Tomcat.addServlet(context, "TributeWebhookServlet", new TributeWebhookServlet());
        context.addServletMappingDecoded("/webhook/tribute", "TributeWebhookServlet");

        tomcat.start();
        tomcat.getServer().await();
    }
}