import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.authentication.AuthXManager;

import java.util.Set;

public class Main {

    public static void main(String[] args) {
        String SCOPES = "https://redis.azure.com/.default";
        String hostname = "DGredis.redis.cache.windows.net";
        int port = 6380;
        // Fill in the app credentials
        String clientId = "";
        String secret = "";
        String tenantId = "";
        TokenAuthConfig tokenAuthConfig = null;

        try {
            tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
                    .scopes(Set.of(SCOPES))
                    .clientId(clientId)
                    .authority("https://login.microsoftonline.com/" + tenantId)
                    .secret(secret)
                    .tokenRequestExecTimeoutInMs(2000)
                    .expirationRefreshRatio(0.75f)
                    .build();

            DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                    .authXManager(new AuthXManager(tokenAuthConfig))
                    .ssl(true)
                    .build();

            System.out.println("=== Testing Jedis Connection ===");
            RedisJedisConnection.testJedis(hostname, port, config);

            System.out.println("=== Testing JDBC Connection ===");
            RedisJDBCConnection.testJDBC(hostname, port, tokenAuthConfig);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
