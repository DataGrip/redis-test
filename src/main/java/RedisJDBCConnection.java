import java.sql.*;
import java.util.Properties;

public class RedisJDBCConnection {

    public static void connect(String hostname, int port, String objectId, String authToken) {

        try {
            Class.forName("jdbc.RedisDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Properties properties = new Properties();
        properties.setProperty("user", objectId);
        properties.setProperty("password", authToken);

        String url = "jdbc:redis://" + hostname + ":" + port + "/0?ssl=true";

        try {
            // Connect to Redis
            Connection connection = DriverManager.getConnection(url, properties);
            System.out.println("Connected to Redis with Redis driver");

            Statement statement = connection.createStatement();

            // Test connection
            ResultSet rs = statement.executeQuery("PING");
            
            if (rs.next()) {
                System.out.println("PING: " + rs.getString(1)); // "PONG"
            } else {
                System.out.println("No response");
                statement.close();
                connection.close();
            }
            rs.close();

            statement.execute("SET test_key \"hello\"");
            ResultSet rs2 = statement.executeQuery("GET test_key");

            if (rs2.next()) {
                System.out.println("Get test_key: " + rs2.getString(1));
            } else {
                System.out.println("Failed to retrieve test value");
                rs2.close();
                statement.close();
                connection.close();
            }
            rs2.close();
            
            System.out.println("Success!");
            
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
