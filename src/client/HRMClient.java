package client;

import common.HRMService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

public class HRMClient {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Properties props = loadConfig();
                String host = props.getProperty("rmi.host", "localhost");
                int port = Integer.parseInt(props.getProperty("rmi.port", "1099"));

                String url = "rmi://" + host + ":" + port + "/HRMService";
                HRMService service = (HRMService) Naming.lookup(url);

                LoginFrame loginFrame = new LoginFrame(service);
                loginFrame.setVisible(true);
            } catch (RemoteException | NotBoundException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Cannot connect to server. Please ensure the server is running and try again.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to start client: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
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
