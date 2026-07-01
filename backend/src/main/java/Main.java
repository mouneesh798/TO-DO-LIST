public class Main {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        int port = Integer.getInteger("server.port", 8080);
        TaskWebServer webServer = new TaskWebServer(manager, port);

        try {
            webServer.start();
            System.out.println("To-Do List is running at http://localhost:" + webServer.getPort());
            System.out.println("Press Ctrl+C to stop the server.");
        } catch (Exception e) {
            System.out.println("Failed to start the server: " + e.getMessage());
        }
    }
}
