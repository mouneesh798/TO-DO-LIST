import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskWebServer {
    private final TaskManager manager;

    public TaskWebServer(TaskManager manager) {
        this.manager = manager;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", this::handleRoot);
        server.createContext("/tasks", this::handleTasks);
        server.setExecutor(null);
        server.start();
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        byte[] response = buildHtml().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String formData = new String(body, StandardCharsets.UTF_8);
            String taskName = formData.replace("task=", "").replace("+", " ");
            manager.addTask(taskName);
            exchange.getResponseHeaders().add("Location", "/");
            exchange.sendResponseHeaders(303, -1);
            return;
        }

        List<Task> tasks = manager.getTasks();
        StringBuilder response = new StringBuilder();
        for (Task task : tasks) {
            response.append(task.toString()).append("\n");
        }
        byte[] data = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String buildHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<title>Java To-Do List</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;max-width:600px;margin:40px auto;padding:20px;}");
        html.append("form{display:flex;gap:10px;margin-bottom:20px;}");
        html.append("input{flex:1;padding:8px;}");
        html.append("button{padding:8px 12px;}");
        html.append("ul{list-style:none;padding:0;}");
        html.append("li{padding:6px 0;}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<h1>Java To-Do List</h1>");
        html.append("<form method='post' action='/tasks'>");
        html.append("<input name='task' placeholder='Enter a task' required />");
        html.append("<button type='submit'>Add Task</button>");
        html.append("</form>");
        html.append("<h2>Current Tasks</h2>");
        html.append("<ul>");
        for (Task task : manager.getTasks()) {
            html.append("<li>").append(task).append("</li>");
        }
        html.append("</ul>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }
}
