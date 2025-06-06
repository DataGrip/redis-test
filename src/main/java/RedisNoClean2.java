import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

public class RedisNoClean2 {
    public static void main(String[] args) {
        String redisHostname = "DGredis.redis.cache.windows.net";
        int redisPort = 6380;

        // Personal Object ID from Azure Portal
        String objectId = "42e8dcac-c4f0-4dd2-841a-604167fc8eee";

        try {
            System.out.println("=== Direct AUTH ===");
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
            System.out.println("Token (clean whitespaces when copying from the console): " + token.getToken());
            System.out.println("Using Object ID: " + objectId);

            // Show whitespaces
            boolean hasWhitespace = originalToken.contains(" ") ||
                    originalToken.contains("\n") ||
                    originalToken.contains("\r") ||
                    originalToken.contains("\t");
            System.out.println("Token contains whitespace/newlines: " + hasWhitespace);

            if (hasWhitespace) {
                System.out.println("⚠️  Token contains whitespace characters that may cause issues on Windows");
            }

            System.out.println("Connecting to Redis host: " + redisHostname + ":" + redisPort);

            DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                    .ssl(true)
                    .user(objectId)  // Object ID as username
                    .password(originalToken)  // Token as password
                    .build();
            Jedis jedis = new Jedis(redisHostname, redisPort, config);

            System.out.println("PING...");
            String pongResponse = jedis.ping();
            System.out.println("Answer: " + pongResponse);

            // Test set/get
            System.out.println("Testing SET/GET:");
            jedis.set("hello", "world from Redis No Clean Method 2 Config!");
            String value = jedis.get("hello");
            System.out.println("GET hello " + value);

            // Test with TTL
            jedis.setex("noclean2:temp-key", 60, "No clean method 2 - expires in 60 seconds");
            System.out.println("GET noclean2:temp-key: " + jedis.get("noclean2:temp-key"));
            System.out.println("TTL noclean2:temp-key: " + jedis.ttl("noclean2:temp-key") + " seconds");

            jedis.close();
            System.out.println("✅ No clean token method 2 successful!");

        } catch (Exception e) {
            System.err.println("❌ No clean token method 2 failed: " + e.getMessage());

            if (e.getMessage().contains("WRONGPASS") || e.getMessage().contains("invalid username-password")) {
                System.err.println("\n🔍 Analysis:");
                System.err.println("The authentication failed, which suggests that:");
                System.err.println("1. The token contains whitespace/newline characters that break authentication on Windows");
                System.err.println("2. Token cleaning (removing \\s+ characters) is necessary for your environment");
                System.err.println("3. Try using one of the other methods with token cleaning");
            }

            e.printStackTrace();

            System.out.println("\n📋 Comparison test:");

        }
    }
}
