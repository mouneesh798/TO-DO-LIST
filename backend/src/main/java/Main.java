public class Main {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        int port = resolvePort();
        TaskWebServer webServer = new TaskWebServer(manager, port);

        try {
            webServer.start();
            System.out.println("To-Do List is running at http://localhost:" + webServer.getPort());
            System.out.println("Press Ctrl+C to stop the server.");
        } catch (Exception e) {
            System.out.println("Failed to start the server: " + e.getMessage());
        }
    }

    private static int resolvePort() {
        String configuredPort = System.getProperty("server.port");
        if (configuredPort == null || configuredPort.isBlank()) {
            configuredPort = System.getenv("PORT");
        }

        if (configuredPort == null || configuredPort.isBlank()) {
            return 8080;
        }

        try {
            return Integer.parseInt(configuredPort);
        } catch (NumberFormatException e) {
            return 8080;
        }
    }
}
