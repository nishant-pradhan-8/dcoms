package server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HRMServer {

    private static final Logger LOGGER = Logger.getLogger(HRMServer.class.getName());

    public static void main(String[] args) {
        try {
            Properties props = loadConfig();
            String host = props.getProperty("rmi.host", "localhost");
            int port = Integer.parseInt(props.getProperty("rmi.port", "1099"));

            System.setProperty("java.rmi.server.hostname", host);

            Registry registry = LocateRegistry.createRegistry(port);
            HRMServiceImpl service = new HRMServiceImpl();
            registry.bind("HRMService", service);

            String message = "HRM Server started. Service bound as 'HRMService' at rmi://"
                    + host + ":" + port + "/HRMService";
            System.out.println(message);
            LOGGER.info(message);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start HRM server", e);
            System.err.println("Failed to start HRM server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties loadConfig() throws IOException {
        Properties props = new Properties();

        try (InputStream in = HRMServer.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                return props;
            }
        }

        Path configPath = Path.of("config.properties");
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
                return props;
            }
        }

        throw new IOException("config.properties not found on classpath or working directory");
    }
}
