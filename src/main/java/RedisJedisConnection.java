import redis.clients.jedis.Jedis;
import redis.clients.jedis.DefaultJedisClientConfig;

public class RedisJedisConnection {

    public static void connect(String hostname, int port, String objectId, String authToken) {
        try {
            // Set config
            DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                    .user(objectId)
                    .password(authToken)
                    .ssl(true)
                    .build();
            
            // Connect to Redis
            Jedis connection = new Jedis(hostname, port, config);
            System.out.println("Connected to Redis with Jedis");

            // Test connection
            String pong = connection.ping();
            System.out.println("PING: " + pong); // "PONG"
            
            if (!"PONG".equals(pong)) {
                System.out.println("PING failed - expected PONG, got: " + pong);
                connection.close();
            }

            // SET key value
            connection.set("test_key", "hello");
            // GET value
            String getKey = connection.get("test_key");
            System.out.println("Get test_key: " + getKey);
            if (!"hello".equals(getKey)) {
                System.out.println("SET/GET test failed: " + getKey);
                connection.close();
            } else {
                System.out.println("Success!");
            }

        } catch (Exception e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}