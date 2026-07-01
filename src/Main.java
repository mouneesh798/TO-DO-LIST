public class Main {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        TaskWebServer webServer = new TaskWebServer(manager);

        try {
            webServer.start();
            System.out.println("To-Do List is running at http://localhost:8080");
            System.out.println("Press Ctrl+C to stop the server.");
        } catch (Exception e) {
            System.out.println("Failed to start the server: " + e.getMessage());
        }
    }
}
