package server;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
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

            configureSsl(props);
            System.setProperty("java.rmi.server.hostname", host);

            RMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            RMIServerSocketFactory ssf = new SslRMIServerSocketFactory();

            Registry registry = LocateRegistry.createRegistry(port, csf, ssf);
            HRMServiceImpl service = new HRMServiceImpl(csf, ssf);
            registry.rebind("HRMService", service);

            String message = "HRM Server started with SSL/TLS. Service bound as 'HRMService' at rmi://"
                    + host + ":" + port + "/HRMService";
            System.out.println(message);
            LOGGER.info(message);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start HRM server", e);
            System.err.println("Failed to start HRM server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void configureSsl(Properties props) {
        String keyStore = props.getProperty("ssl.keystore", "ssl/server-keystore.jks");
        String keyStorePassword = props.getProperty("ssl.keystore.password", "changeit");

        Path keyStorePath = Path.of(keyStore);
        if (!Files.exists(keyStorePath)) {
            throw new IllegalStateException(
                    "SSL keystore not found: " + keyStorePath.toAbsolutePath()
                            + ". Run: bash ssl/generate-certs.sh");
        }

        System.setProperty("javax.net.ssl.keyStore", keyStorePath.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);

        // Trust own cert so registry/stub SSL handshakes can complete on the server side.
        String trustStore = props.getProperty("ssl.truststore", "ssl/client-truststore.jks");
        String trustStorePassword = props.getProperty("ssl.truststore.password", keyStorePassword);
        Path trustStorePath = Path.of(trustStore);
        if (Files.exists(trustStorePath)) {
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        }

        LOGGER.info("SSL keystore configured: " + keyStorePath.toAbsolutePath());
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
