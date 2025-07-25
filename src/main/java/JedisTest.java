import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.DefaultJedisClientConfig;

import java.io.*;
import java.time.OffsetDateTime;

public class JedisTest {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) {
        try {
            System.setProperty("org.slf4j.simpleLogger.log.com.azure", "warn");
            System.setProperty("org.slf4j.simpleLogger.log.reactor.netty", "warn");

            System.out.println("=== Getting token from Microsoft Entra ID ===\n");

            String redisHostname = promptWithDefault("Redis hostname", "DGredis.redis.cache.windows.net");
            String objectId = promptWithDefault("Object ID", "my_id");
            int redisPort = Integer.parseInt(promptWithDefault("Redis port", "6380"));
            String sslEnabled = promptWithDefault("SSL enabled", "true");

            // Getting token from Azure Entra ID
            var credential = new DefaultAzureCredentialBuilder().build();
            var tokenContext = new TokenRequestContext().addScopes("https://redis.azure.com/.default");
            AccessToken token = credential.getToken(tokenContext).block();

            if (token == null) {
                System.err.println("Failed to get token from Azure. \n");
                return;
            }

            String authToken = token.getToken();
            OffsetDateTime expires = token.getExpiresAt();

            System.out.println("Successfully retrieved token (expires at " + expires + ") \n");

            // Saving token to the file
            try (FileWriter writer = new FileWriter("jedis_token.txt")) {
                writer.write(authToken);
                System.out.println("Token saved to jedis_token.txt");
            }

            // Connection settings
            JedisClientConfig config = DefaultJedisClientConfig.builder()
                    .ssl(Boolean.parseBoolean(sslEnabled))
                    .user(objectId)
                    .password(authToken)
                    .build();

            // Connecting to Redis
            try (Jedis jedis = new Jedis(redisHostname, redisPort, config)) {
                System.out.println("Connected to Redis \n");

                // Test PING
                System.out.println("Test: PING");
                String pingResponse = jedis.ping();
                System.out.println("response: " + pingResponse + "\n");

                // Test SET/GET
                System.out.println("Test: SET/GET");
                jedis.set("hello", "world from Azure Redis Cache!");
                String value = jedis.get("hello");
                System.out.println("GET hello: " + value + "\n");
            }

            System.out.println("Redis tests completed successfully.");

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String promptWithDefault(String message, String defaultValue) throws IOException {
        System.out.print(message + " [" + defaultValue + "]: ");
        String input = reader.readLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
}
