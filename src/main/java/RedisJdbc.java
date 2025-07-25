import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

import java.io.FileWriter;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Properties;

public class RedisJdbc {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) {
        try {
            System.setProperty("org.slf4j.simpleLogger.log.com.azure", "warn");
            System.setProperty("org.slf4j.simpleLogger.log.reactor.netty", "warn");

            System.out.println("=== Getting token MS Entra ID ===\n");
            // Interactive input for connection parameters
            String redisHostname = promptWithDefault("Redis hostname", "DGredis.redis.cache.windows.net");
            String objectId = promptWithDefault("Object ID", "my_id");
            int redisPort = Integer.parseInt(promptWithDefault("Redis port", "6380"));
            String databaseName = promptWithDefault("Database", "0");
            String sslEnabled = promptWithDefault("SSL enabled", "true");

            // Create Azure credential
            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

            // Get token for Redis
            TokenRequestContext tokenContext = new TokenRequestContext()
                    .addScopes("https://redis.azure.com/.default");

            AccessToken token = credential.getToken(tokenContext).block();

            if (token == null) {
                System.err.println("Failed getting token!\n");
                return;
            }

            System.out.println("Successfully retrieved token!\n");

            // Show token
            String originalToken = token.getToken();

            // Save token to a file
            try (FileWriter writer = new FileWriter("redis_token.txt")) {
                writer.write(originalToken);
                System.out.println("Token saved to redis_token.txt");
            } catch (IOException e) {
                System.err.println("Failed to write token to file: " + e.getMessage());
                return;
            }

            System.out.println("expires on: " + token.getExpiresAt() + "\n");

            // JDBC driver registration
            try {
                Class.forName("jdbc.RedisDriver");
                System.out.println("Redis JDBC Driver loaded successfully! \n");
            } catch (ClassNotFoundException e) {
                System.err.println("Redis JDBC Driver not found in classpath! \n");
                System.err.println("Make sure your redis-jdbc-driver-1.5.jar is in classpath \n");
                throw e;
            }

            System.out.println("Connecting to Redis host: " + redisHostname + ":" + redisPort);
            System.out.println("using Object ID: " + objectId + "\n");

            // JDBC connection
            String jdbcUrl = "jdbc:redis://" + redisHostname + ":" + redisPort  + "/" + databaseName + "?ssl=true";

            // Create Properties for authentication
            Properties props = new Properties();
            props.setProperty("user", objectId);  // Object ID as username
            props.setProperty("password", originalToken);  // Token as password
            props.setProperty("ssl", sslEnabled);

            Connection connection = DriverManager.getConnection(jdbcUrl, props);
            System.out.println("JDBC URL: " + jdbcUrl);
            System.out.println("JDBC Connection established! \n");

            // Test connection
            if (!connection.isClosed()) {
                System.out.println("Connection is active: ");

                // Get metadata
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("Database: " + metaData.getDatabaseProductName());
                System.out.println("Database version: " + metaData.getDatabaseProductVersion());
                System.out.println("Driver: " + metaData.getDriverName() + " v" + metaData.getDriverVersion());
                System.out.println("SSL: " + sslEnabled);
            }
            System.out.println("\n");
            Statement statement = connection.createStatement();

            // PING test
            System.out.println("PING test:");
            try {
                ResultSet rs = statement.executeQuery("PING");
                if (rs.next()) {
                    System.out.println("Answer: " + rs.getString(1) + "\n");
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println("PING via SQL not supported: " + e.getMessage() + "\n");
            }

            // SQL test
            System.out.println("SQL test:");
            ResultSet res = statement.executeQuery("GET mykey");
            while (res.next()) {
                System.out.println("mykey: " + res.getString(1) + "\n");
            }
            res.close();

            // Test SET/GET
            System.out.println("Testing SET/GET:");
            try {
                statement.executeUpdate("SET hello 'world from Redis!'");
                ResultSet rs = statement.executeQuery("GET hello");
                if (rs.next()) {
                    String value = rs.getString(1);
                    System.out.println("GET hello: " + value + "\n");
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println("SET/GET via SQL not supported: " + e.getMessage() + "\n");

                //  PreparedStatement
                try {
                    PreparedStatement setStmt = connection.prepareStatement("SET ? ?");
                    setStmt.setString(1, "hello");
                    setStmt.setString(2, "world from Redis!");
                    setStmt.executeUpdate();
                    setStmt.close();

                    PreparedStatement getStmt = connection.prepareStatement("GET ?");
                    getStmt.setString(1, "hello");
                    ResultSet rs = getStmt.executeQuery();
                    if (rs.next()) {
                        String value = rs.getString(1);
                        System.out.println("GET hello (via PreparedStatement): " + value + "\n");
                    }
                    rs.close();
                    getStmt.close();
                } catch (SQLException e2) {
                    System.out.println("PreparedStatement also not supported: " + e2.getMessage() + "\n");
                }
            }

            // Clean up
            statement.close();
            connection.close();
            System.out.println("Successful!");

        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            System.err.println("\n Solution:");
            System.err.println("1. Add redis-jdbc.jar to your classpath");
            System.err.println("2. Verify the driver class name (might be different)");
            System.err.println("3. Check if you have the correct Redis JDBC driver");
        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());

            if (e.getMessage().contains("WRONGPASS") || e.getMessage().contains("invalid username-password") ||
                    e.getMessage().contains("authentication") || e.getMessage().contains("login")) {
                System.err.println("\n🔍 Analysis:");
                System.err.println("The authentication failed, which suggests that:");
                System.err.println("1. The token contains whitespace/newline characters that break authentication");
                System.err.println("2. Token cleaning (removing \\s+ characters) might be necessary");
                System.err.println("3. JDBC driver might require different authentication format");
                System.err.println("4. Check if JDBC driver supports Microsoft Entra ID authentication");
                System.err.println("5. Try 'mvn versions:display-dependency-updates' to check for updates");

            }

            e.printStackTrace();
            System.out.println("\n Troubleshooting:");
            System.out.println("1. Verify Redis JDBC driver supports Azure Cache for Redis");
            System.out.println("2. Check driver documentation for authentication methods");
            System.out.println("3. Try different JDBC URL formats");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n Comparison test:");
            System.out.println("This error suggests the Redis JDBC driver might not be compatible");
            System.out.println("with Azure Cache for Redis or Microsoft Entra ID authentication");
        }
    }
    // Utility methods for interactive input
    private static String prompt(String message) throws Exception {
        System.out.print(message + ": ");
        return reader.readLine().trim();
    }

    private static String promptWithDefault(String message, String defaultValue) throws Exception {
        System.out.print(message + " [" + defaultValue + "]: ");
        String input = reader.readLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    private static boolean promptYesNo(String message, boolean defaultValue) throws Exception {
        String defaultStr = defaultValue ? "Y/n" : "y/N";
        System.out.print(message + " [" + defaultStr + "]: ");
        String input = reader.readLine().trim().toLowerCase();

        if (input.isEmpty()) {
            return defaultValue;
        }
        return input.equals("y") || input.equals("yes");
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        if (value != null && !value.trim().isEmpty()) {
            System.out.println("Using " + envVar + " from environment");
            return value.trim();
        } else {
            System.out.println(envVar + " not found, using default: " + defaultValue);
            return defaultValue;
        }
    }
}