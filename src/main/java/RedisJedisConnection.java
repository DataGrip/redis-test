import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;


public class RedisJedisConnection {

    public static void testJedis(String hostname, int port, DefaultJedisClientConfig config) {

        try {
            UnifiedJedis jedis = new UnifiedJedis(
                    new HostAndPort(hostname, port),
                    config);
            System.out.println("Connected to Redis with Jedis");

            // Tests
            System.out.println("PING: " + jedis.ping());
            if (!"PONG".equals(jedis.ping())) {
                System.out.println("No response from the host, got: " + jedis.ping());
                jedis.close();
            }

            System.out.println(String.format("Database size is %d", jedis.dbSize()));

            jedis.set("test_key", "hello");
            System.out.println("GET test key: " + jedis.get("test_key"));
            if (!"hello".equals(jedis.get("test_key"))) {
                System.out.println("SET/GET test failed: " + jedis.get("test_key"));
            }

            jedis.close();

        } catch (Exception e) {
            throw new RuntimeException("Redis connection failed: " + e.getMessage(), e);
        }

    }
}
