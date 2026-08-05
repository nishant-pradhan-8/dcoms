package client;

import common.HRMService;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

public class HRMClient {

    private static final String[] CONNECT_OPTIONS = {"Reconnect", "Quit"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HRMClient::startClient);
    }

    private static void startClient() {
        Properties props;
        try {
            props = loadConfig();
            configureSsl(props);
        } catch (Exception e) {
            if (askReconnect(
                    "Failed to load client configuration or SSL settings.\n\n" + e.getMessage()
                            + "\n\nFix the issue, then click Reconnect.")) {
                startClient();
            }
            return;
        }

        String host = props.getProperty("rmi.host", "localhost");
        int port = Integer.parseInt(props.getProperty("rmi.port", "1099"));

        while (true) {
            try {
                HRMService service = lookupService(host, port);
                LoginFrame loginFrame = new LoginFrame(service);
                loginFrame.setVisible(true);
                return;
            } catch (RemoteException | NotBoundException e) {
                boolean retry = askReconnect(
                        "Cannot connect to SSL RMI server at rmi://" + host + ":" + port
                                + "/HRMService.\n\n"
                                + "Please ensure the server is running and SSL certificates are configured.\n\n"
                                + e.getMessage());
                if (!retry) {
                    return;
                }
            } catch (Exception e) {
                boolean retry = askReconnect(
                        "Failed to start client:\n\n" + e.getMessage());
                if (!retry) {
                    return;
                }
            }
        }
    }

    private static boolean askReconnect(String message) {
        int choice = JOptionPane.showOptionDialog(
                null,
                message,
                "Connection Error",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                CONNECT_OPTIONS,
                CONNECT_OPTIONS[0]
        );
        return choice == 0;
    }

    private static HRMService lookupService(String host, int port)
            throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, port, new SslRMIClientSocketFactory());
        return (HRMService) registry.lookup("HRMService");
    }

    private static void configureSsl(Properties props) {
        String trustStore = props.getProperty("ssl.truststore", "ssl/client-truststore.jks");
        String trustStorePassword = props.getProperty("ssl.truststore.password", "changeit");

        Path trustStorePath = Path.of(trustStore);
        if (!Files.exists(trustStorePath)) {
            throw new IllegalStateException(
                    "SSL truststore not found: " + trustStorePath.toAbsolutePath()
                            + ". Run: bash ssl/generate-certs.sh");
        }

        System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
    }

    private static Properties loadConfig() throws IOException {
        Properties props = new Properties();

        try (InputStream in = HRMClient.class.getClassLoader().getResourceAsStream("config.properties")) {
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
