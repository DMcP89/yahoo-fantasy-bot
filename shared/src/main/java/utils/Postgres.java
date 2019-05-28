package utils;

import com.github.scribejava.core.model.OAuth2AccessToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.sql.*;

public class Postgres {
    private static final Logger log = LogManager.getLogger(Postgres.class);

    private static Connection connection = null;

    /**
     * Gets the connection to the DB.  If there are 100 attempts, and none are successful, the application will exit.
     *
     * @return connection to DB
     */
    private static Connection getConnection() {
        int attempts = 0;

        while (connection == null) {
            if (attempts >= 100) {
                log.fatal("There have been 100 attempts to connect to the DB.  " +
                        "None have been successful.  Exiting.", new Throwable());
                System.exit(-1);
            }

            try {
                log.trace("Creating connection to database...");

                connection = DriverManager.getConnection(EnvHandler.JDBC_DATABASE_URL.getValue());

                log.debug("Connection established to database.");

                return connection;
            } catch (SQLException e) {
                log.error(e.getLocalizedMessage(), new Throwable());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    log.error(e.getLocalizedMessage(), new Throwable());
                }
                attempts++;
            }
        }

        return connection;
    }

    /**
     * Saves the Yahoo token data to the DB.
     *
     * @param token token to be saved
     */
    public static void saveTokenData(OAuth2AccessToken token) {
        try {
            getConnection();

            log.trace("Attempting to save token data...");

            final String refreshToken = token.getRefreshToken();
            final String retrievedTime = Long.toString(System.currentTimeMillis());
            final String rawResponse = token.getRawResponse();
            final String tokenType = token.getTokenType();
            final String accessToken = token.getAccessToken();
            final String expiresIn = token.getExpiresIn().toString();
            final String scope = token.getScope();

            final Statement statement = connection.createStatement();
            final String sql = "INSERT INTO tokens (\"yahooRefreshToken\",\"yahooTokenRetrievedTime\",\"yahooTokenRawResponse\",\"yahooTokenType\",\"yahooAccessToken\", \"yahooTokenExpireTime\", \"yahooTokenScope\")" + " VALUES (\'" + refreshToken + "\',\'" + retrievedTime + "\',\'" + rawResponse + "\',\'" + tokenType + "\',\'" + accessToken + "\',\'" + expiresIn + "\'," + scope + ")";

            statement.executeUpdate(sql);

            log.trace("Token data has been saved.");

            return;
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        }

    }

    /**
     * Saves the last time data was checked.
     */
    public static void saveLastTimeChecked() {
        try {
            getConnection();

            log.trace("Attempting to save last time checked...");

            final Statement statement = connection.createStatement();
            final String sql = "INSERT INTO latest_time (\"latest_time\")" + " VALUES (\'" + (System.currentTimeMillis() / 1000) + "\')";

            statement.executeUpdate(sql);

            log.trace("Latest time has been saved.");
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    /**
     * Marks that the start up message has been received by the users.  This is so that the message is not sent every-time the application is started.
     */
    public static void markStartupMessageSent() {
        try {
            getConnection();

            log.trace("Marking startup message sent...");

            final Statement statement = connection.createStatement();
            final String sql = "INSERT INTO start_up_message_received (\"was_received\")" + " VALUES (\'" + true + "\')";

            statement.executeUpdate(sql);

            log.trace("Startup message marked sent.");
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    /**
     * Checks to see if the startup message was sent to the users.
     *
     * @return whether or not the startup message was sent
     */
    public static boolean wasStartUpMessageSent() {
        try {
            getConnection();

            final Statement statement = connection.createStatement();
            final ResultSet row = statement.executeQuery("SELECT * FROM start_up_message_received ORDER BY \"was_received\" DESC LIMIT 1");

            if (row.next()) {
                return row.getBoolean("was_received");
            }

            return false;
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Gets the latest time checked.
     *
     * @return long of the time
     */
    public static long getLatestTimeChecked() {
        dropTopRows("latest_time", "latest_time");
        try {
            getConnection();

            final Statement statement = connection.createStatement();
            final ResultSet row = statement.executeQuery("SELECT * FROM latest_time ORDER BY \"latest_time\" DESC LIMIT 1");

            if (row.next()) {
                return row.getLong("latest_time");
            }

            return System.currentTimeMillis() / 1000;
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
            return System.currentTimeMillis() / 1000;
        }
    }

    /**
     * Gets the latest token data
     *
     * @return token data
     */
    public static PostgresToken getLatestTokenData() {
        dropTopRows("tokens", "yahooTokenRetrievedTime");

        try {
            getConnection();

            final Statement statement = connection.createStatement();
            final ResultSet row = statement.executeQuery("SELECT * FROM tokens ORDER BY \"yahooTokenRetrievedTime\" DESC LIMIT 1");

            if (row.next()) {
                final String refreshToken = row.getString("yahooRefreshToken");
                final long retrievedTime = row.getLong("yahooTokenRetrievedTime");
                final String rawResponse = row.getString("yahooTokenRawResponse");
                final String tokenType = row.getString("yahooTokenType");
                final String accessToken = row.getString("yahooAccessToken");
                final long expiresIn = row.getLong("yahooTokenExpireTime");
                final String scope = row.getString("yahooTokenScope");

                return new PostgresToken(new OAuth2AccessToken(accessToken, tokenType, (int) expiresIn, refreshToken, scope, rawResponse), retrievedTime);
            }
            return null;
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage(), new Throwable());
            return null;
        }
    }

    /**
     * Drops the top rows of a given table.
     *
     * @param tableName table name
     * @param orderBy   the column to order by
     */
    private static void dropTopRows(String tableName, String orderBy) {
        try {
            getConnection();
            final Statement statement = connection.createStatement();
            final ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);

            if (row.next()) {
                int count = row.getInt(1);

                if (count > 20) {
                    log.trace("More than 20 entries in the " + tableName + " table.  Removing top 20.");
                    statement.execute("DELETE\n" +
                            "FROM " + tableName + "\n" +
                            "WHERE ctid IN (\n" +
                            "        SELECT ctid\n" +
                            "        FROM " + tableName + "\n" +
                            "        ORDER BY \"" + orderBy + "\" limit 20\n" +
                            "        )");
                }
            }

        } catch (SQLException e) {
            log.error(e.getLocalizedMessage(), new Throwable());
        }
    }
}
