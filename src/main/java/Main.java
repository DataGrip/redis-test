import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        // Fill in your credentials
        String hostname = "DGredis.redis.cache.windows.net";
        String objectId = "42e8dcac-c4f0-4dd2-841a-604167fc8eee"; // Your user ID in Azure
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
        RedisJDBCConnection.connect(hostname, port, objectId, authToken);
        System.out.println();
        RedisJedisConnection.connect(hostname, port, objectId, authToken);
    }
}
