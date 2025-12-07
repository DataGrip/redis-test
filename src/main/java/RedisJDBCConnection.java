import java.sql.*;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.jedis.authentication.AuthXManager;

public class RedisJDBCConnection {

    public static void testJDBC(String hostname, int port, TokenAuthConfig tokenAuthConfig) {
        String objectId = ""; // set app objectID
        try {
            Class.forName("jdbc.RedisDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Properties properties = new Properties();

        String url = "jdbc:redis://" + hostname + ":" + port + "/0?ssl=true";

        // Get token from callback
        AtomicReference<String> tokenHolder = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        AuthXManager authXManager = new AuthXManager(tokenAuthConfig);

        authXManager.addPostAuthenticationHook((token) -> {
            String accessToken = token.getValue();
            tokenHolder.set(accessToken);
            latch.countDown();
        });

        authXManager.start();

        String token = tokenHolder.get().toString();
        System.out.println("Token: " + token);

        properties.setProperty("user", objectId);
        properties.setProperty("password", token);

        try {
            // Connect to Redis
            Connection connection = DriverManager.getConnection(url, properties);
            System.out.println("Connected to Redis with Redis driver");

            Statement statement = connection.createStatement();

            // Tests
            try (ResultSet rs = statement.executeQuery("PING")) {
                if (!rs.next()) {
                    System.out.println("No response from the host");
                    statement.close();
                    connection.close();
                    return;
                }
                System.out.println("PING: " + rs.getString(1));
            }

            statement.execute("SET test_key \"hello\"");

            try (ResultSet rs = statement.executeQuery("GET test_key")) {
                if (!rs.next()) {
                    System.out.println("Failed to retrieve test value");
                    statement.close();
                    connection.close();
                    return;
                }
                System.out.println("GET test_key: " + rs.getString(1));
            }

            System.out.println("Success!");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}