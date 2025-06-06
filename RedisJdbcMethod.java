import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

import java.sql.*;
import java.util.Properties;

public class RedisJdbcMethod {
    public static void main(String[] args) {
        String redisHostname = "DGredis.redis.cache.windows.net";
        int redisPort = 6380;
        int databaseName = 0;

        // Personal Object ID from Azure Portal
        String objectId = "42e8dcac-c4f0-4dd2-841a-604167fc8eee";

        try {
            System.out.println("=== JDBC Direct AUTH ===");
            System.out.println("Getting token MS Entra ID...");

            // Create Azure credential
            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

            // Get token for Redis
            TokenRequestContext tokenContext = new TokenRequestContext()
                    .addScopes("https://redis.azure.com/.default");

            AccessToken token = credential.getToken(tokenContext).block();

            if (token == null) {
                System.err.println("Failed getting token!");
                return;
            }

            System.out.println("Successfully retrieved token!");

            // Show token
            String originalToken = token.getToken();
            System.out.println("Token (clean whitespaces if copy from the console): " + token.getToken());
            System.out.println("Using Object ID: " + objectId);

            System.out.println("Connecting to Redis host: " + redisHostname + ":" + redisPort);

            // JDBC driver registration
            try {
                Class.forName("jdbc.RedisDriver");
                System.out.println("✅ Redis JDBC Driver loaded successfully");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ Redis JDBC Driver not found in classpath!");
                System.err.println("Make sure your redis-jdbc.jar is in classpath");
                throw e;
            }

            // JDBC connection
            String jdbcUrl = "jdbc:redis://" + redisHostname + ":" + redisPort  + "/" + databaseName + "?ssl=true";
            System.out.println("JDBC URL: " + jdbcUrl);

            // Properties for AUTH
            Properties props = new Properties();
            props.setProperty("user", objectId);  // Object ID as username
            props.setProperty("password", originalToken);  // Token as password
            props.setProperty("ssl", "true");

            Connection connection = DriverManager.getConnection(jdbcUrl, props);
            System.out.println("✅ JDBC Connection established");

            // Connection
            if (!connection.isClosed()) {
                System.out.println("Connection is active");

                // Get metadata
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("Database: " + metaData.getDatabaseProductName());
                System.out.println("Driver: " + metaData.getDriverName() + " v" + metaData.getDriverVersion());
            }

            Statement statement = connection.createStatement();

            // PING test
            try {
                System.out.println("PING...");
                ResultSet rs = statement.executeQuery("PING");
                if (rs.next()) {
                    System.out.println("Answer: " + rs.getString(1));
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println("PING via SQL not supported: " + e.getMessage());
            }

            // Test SET/GET
            System.out.println("Testing SET/GET:");
            try {
                // SET operation
                statement.executeUpdate("SET hello 'world from Redis JDBC No Clean Method!'");
                System.out.println("SET operation completed");

                // GET operation
                ResultSet rs = statement.executeQuery("GET hello");
                if (rs.next()) {
                    String value = rs.getString(1);
                    System.out.println("GET hello " + value);
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println("SET/GET via SQL not supported: " + e.getMessage());

                // Альтернативный способ через PreparedStatement
                try {
                    PreparedStatement setStmt = connection.prepareStatement("SET ? ?");
                    setStmt.setString(1, "hello");
                    setStmt.setString(2, "world from Redis JDBC Method!");
                    setStmt.executeUpdate();
                    setStmt.close();

                    PreparedStatement getStmt = connection.prepareStatement("GET ?");
                    getStmt.setString(1, "hello");
                    ResultSet rs = getStmt.executeQuery();
                    if (rs.next()) {
                        String value = rs.getString(1);
                        System.out.println("GET hello (via PreparedStatement): " + value);
                    }
                    rs.close();
                    getStmt.close();
                } catch (SQLException e2) {
                    System.out.println("PreparedStatement also not supported: " + e2.getMessage());
                }
            }

            // Test with TTL
            try {
                statement.executeUpdate("SETEX jdbc:temp-key 60 'JDBC method - expires in 60 seconds'");
                ResultSet rs = statement.executeQuery("GET jdbc:temp-key");
                if (rs.next()) {
                    System.out.println("GET jdbc:temp-key: " + rs.getString(1));
                }
                rs.close();

                // TTL check
                rs = statement.executeQuery("TTL jdbc:temp-key");
                if (rs.next()) {
                    System.out.println("TTL jdbc:temp-key: " + rs.getString(1) + " seconds");
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println("SETEX/TTL operations not supported: " + e.getMessage());
            }

            // Clean up
            statement.close();
            connection.close();
            System.out.println("✅ JDBC No clean token method successful!");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ JDBC Driver not found: " + e.getMessage());
            System.err.println("\n📋 Solution:");
            System.err.println("1. Add redis-jdbc.jar to your classpath");
            System.err.println("2. Verify the driver class name (might be different)");
            System.err.println("3. Check if you have the correct Redis JDBC driver");
        } catch (SQLException e) {
            System.err.println("❌ JDBC No clean token method failed: " + e.getMessage());

            if (e.getMessage().contains("WRONGPASS") || e.getMessage().contains("invalid username-password") ||
                    e.getMessage().contains("authentication") || e.getMessage().contains("login")) {
                System.err.println("\n🔍 Analysis:");
                System.err.println("The authentication failed, which suggests that:");
                System.err.println("1. The token contains whitespace/newline characters that break authentication");
                System.err.println("2. Token cleaning (removing \\s+ characters) might be necessary");
                System.err.println("3. JDBC driver might require different authentication format");
                System.err.println("4. Check if JDBC driver supports Microsoft Entra ID authentication");
            }

            e.printStackTrace();
            System.out.println("\n📋 Troubleshooting:");
            System.out.println("1. Verify Redis JDBC driver supports Azure Cache for Redis");
            System.out.println("2. Check driver documentation for authentication methods");
            System.out.println("3. Try different JDBC URL formats");
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n📋 Comparison test:");
            System.out.println("This error suggests the Redis JDBC driver might not be compatible");
            System.out.println("with Azure Cache for Redis or Microsoft Entra ID authentication");
        }
    }
}