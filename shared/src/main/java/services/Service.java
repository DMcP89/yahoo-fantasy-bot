package services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Service implements Runnable {
    private static final Logger log = LogManager.getLogger(Service.class);

    private boolean isActivated;

    final String apiURL;
    private String currentMessage;
    private final String name;

    Service(String name, String apiURL) {
        this.name = name;
        this.apiURL = apiURL;
    }

    /**
     * Creates a message.  Recursively creates messages when they are too large.
     *
     * @param message the message to be sent
     */
    abstract void createMessage(String message);

    /**
     * Sends a message to the group.
     *
     * @param message the message to send
     */
    abstract void sendMessage(String message);

    String correctMessage(String message) {
        try {
            Thread.sleep(1000);
            if (message.endsWith("\\")) {
                message = message.substring(0, message.length() - 1);
            } else if (message.startsWith("n")) {
                message = message.substring(1);
            }

            return message;
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage(), new Throwable());

            return null;
        }
    }

    public void setCurrentMessage(String currentMessage) {
        this.currentMessage = currentMessage;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    @Override
    public void run() {
        if (currentMessage != null) {
            createMessage(currentMessage);
            currentMessage = null;
        }
    }

    @Override
    public String toString() {
        return "\nService: " + name +
                "\nActivated: " + isActivated +
                "\nAPI URL: " + apiURL +
                "\n===";
    }
}
