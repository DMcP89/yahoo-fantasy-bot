package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.Discord;
import services.GroupMe;
import services.Service;
import services.Slack;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServicesHandler {
    private static final Logger log = LogManager.getLogger(ServicesHandler.class);

    private static final GroupMe groupMe = new GroupMe();
    private static final Discord discord = new Discord();
    private static final Slack slack = new Slack();

    private static final Service[] services = {groupMe, discord, slack};
    private static final ExecutorService executorService = Executors.newFixedThreadPool(services.length); // TODO: Need to shut this down when the bot exits

    private static boolean checkedEnvVariables = false;

    // TODO: Need someway to validate the services (env variables set up correctly, fallback, etc.)

    public static void sendMessage(String message) {
        for (Service s : services) {
            s.setCurrentMessage(message);
            executorService.submit(s);
        }
    }

    /**
     * Checks environment variables and prints out their values.  If the {@link services.Service} value is set, then
     * the services is set to activated.  If not, then it skips over that service.
     */
    private static void checkEnvVariables() {
        if (!checkedEnvVariables) {
            log.debug("Checking environment variables.");

            TimeZoneData.checkTimezoneEnv();

            for (EnvHandler e : EnvHandler.values()) {
                log.info(e + ":" + e.getValue());
                switch (e) {
                    case GROUP_ME_ACCESS_TOKEN:
                    case GROUP_ME_BOT_ID:
                    case GROUP_ME_GROUP_ID:
                        if (e.getValue() != null) {
                            groupMe.setActivated(true);
                        }
                        break;
                    case DISCORD_WEBHOOK_URL:
                        if (e.getValue() != null) {
                            discord.setActivated(true);
                        }
                        break;
                    case SLACK_WEBHOOK_URL:
                        if (e.getValue() != null) {
                            slack.setActivated(true);
                        }
                        break;
                    default:
                        break;
                }
            }

            logServices();

            checkedEnvVariables = true;
        }
    }

    public static void startupCheck() {
        checkEnvVariables();
        startupMessages();
    }

    /**
     * Startup messages that should be sent or not.
     */
    private static void startupMessages() {
        if (!Postgres.wasStartUpMessageSent()) {
            sendMessage("Hi there! It looks like this is the first time I am being started!  " +
                    "I can tell you about transactions that have happened, weekly matchup data, and score updates.");
            Postgres.markStartupMessageSent();

            log.trace("Startup message has been sent.");
        } else {
            if (("TRUE").equalsIgnoreCase(EnvHandler.RESTART_MESSAGE.getValue())) {
                sendMessage("Hi there! It looks like I was just restarted.  You may get data from earlier times.");

                log.trace("Restart message has been sent.");
            }
        }
    }

    /**
     * Prints out the services and their respective data.
     */
    private static void logServices() {
        for (Service s : services) {
            System.out.println(s);
        }
    }
}
