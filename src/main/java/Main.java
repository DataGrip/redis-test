import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        // Fill in your credentials
        String hostname = "DGredis.redis.cache.windows.net";  // Redis hostname
        String objectId = ""; // Set your Object ID
        int port = 6380;

        JsonNode root = null;

        try {
            String json = new String(Files.readAllBytes(Paths.get(".envToken")));
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String authToken = root.get("accessToken").asText();

        // Results
        RedisJedisConnection.connect(hostname, port, objectId, authToken);
        System.out.println();
        RedisJDBCConnection.connect(hostname, port, objectId, authToken);

    }
}
