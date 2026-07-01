import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class TaskWebServer {
    private final TaskManager manager;
    private final int port;
    private final Path frontendDirectory;
    private HttpServer httpServer;

    public TaskWebServer(TaskManager manager) {
        this(manager, 8080);
    }

    public TaskWebServer(TaskManager manager, int port) {
        this.manager = manager;
        this.port = port;
        this.frontendDirectory = findFrontendDirectory();
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        httpServer.createContext("/api/tasks", this::handleTaskApi);
        httpServer.createContext("/api/tasks/", this::handleTaskApi);
        httpServer.createContext("/tasks", this::handleLegacyTasks);
        httpServer.createContext("/tasks/complete", this::handleLegacyCompleteTask);
        httpServer.createContext("/tasks/delete", this::handleLegacyDeleteTask);
        httpServer.createContext("/", this::handleStaticFile);
        httpServer.setExecutor(null);
        httpServer.start();
    }

    public int getPort() {
        if (httpServer != null) {
            return httpServer.getAddress().getPort();
        }
        return port;
    }

    private Path findFrontendDirectory() {
        Path fromBackend = Paths.get("..", "frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(fromBackend)) {
            return fromBackend;
        }

        return Paths.get("frontend").toAbsolutePath().normalize();
    }

    private void handleStaticFile(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || "/".equals(requestPath)) {
            requestPath = "/index.html";
        }

        Path file = frontendDirectory.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(frontendDirectory) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "Not Found");
            return;
        }

        byte[] response = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType(file));
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleTaskApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            handleCorsPreflight(exchange);
            return;
        }

        setCorsHeaders(exchange);

        if ("/api/tasks".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, tasksToJson());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                String taskName = parseTaskNameFromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                manager.addTask(taskName);
                sendJson(exchange, 201, tasksToJson());
                return;
            }
        }

        if (path.matches("/api/tasks/\\d+/complete") && "POST".equalsIgnoreCase(method)) {
            int taskIndex = parsePathIndex(path, "/api/tasks/", "/complete");
            manager.markCompleted(taskIndex);
            sendJson(exchange, 200, tasksToJson());
            return;
        }

        if (path.matches("/api/tasks/\\d+") && "DELETE".equalsIgnoreCase(method)) {
            int taskIndex = parsePathIndex(path, "/api/tasks/", "");
            manager.deleteTask(taskIndex);
            sendJson(exchange, 200, tasksToJson());
            return;
        }

        sendJson(exchange, 404, "{\"error\":\"Not found\"}");
    }

    private void handleLegacyTasks(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            manager.addTask(parseTaskNameFromForm(formData));
            redirectHome(exchange);
            return;
        }

        StringBuilder response = new StringBuilder();
        for (Task task : manager.getTasks()) {
            response.append(task).append("\n");
        }
        sendText(exchange, 200, response.toString());
    }

    private void handleLegacyCompleteTask(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            int taskIndex = parseTaskIndexFromForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            manager.markCompleted(taskIndex);
        }
        redirectHome(exchange);
    }

    private void handleLegacyDeleteTask(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            int taskIndex = parseTaskIndexFromForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            manager.deleteTask(taskIndex);
        }
        redirectHome(exchange);
    }

    private String tasksToJson() {
        List<Task> tasks = manager.getTasks();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{")
                    .append("\"id\":").append(i + 1).append(",")
                    .append("\"name\":\"").append(escapeJson(task.getTaskName())).append("\",")
                    .append("\"completed\":").append(task.isCompleted())
                    .append("}");
        }
        json.append("]");
        return json.toString();
    }

    private String parseTaskNameFromJson(String body) {
        String key = "\"task\"";
        int keyIndex = body.indexOf(key);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = body.indexOf(":", keyIndex + key.length());
        int valueStart = body.indexOf("\"", colonIndex + 1);
        if (colonIndex < 0 || valueStart < 0) {
            return "";
        }

        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = valueStart + 1; i < body.length(); i++) {
            char current = body.charAt(i);
            if (escaped) {
                value.append(unescapeJsonChar(current));
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                break;
            } else {
                value.append(current);
            }
        }
        return value.toString();
    }

    private char unescapeJsonChar(char value) {
        return switch (value) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> value;
        };
    }

    private int parsePathIndex(String path, String prefix, String suffix) {
        String value = path.substring(prefix.length(), path.length() - suffix.length());
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String parseTaskNameFromForm(String formData) {
        for (String pair : formData.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "task".equals(parts[0])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private int parseTaskIndexFromForm(String formData) {
        for (String pair : formData.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "index".equals(parts[0])) {
                try {
                    return Integer.parseInt(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(current);
            }
        }
        return escaped.toString();
    }

    private String contentType(Path file) {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        return "application/octet-stream";
    }

    private void sendJson(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendText(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
        } else {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        }
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Vary", "Origin");
    }

    private boolean isAllowedOrigin(String origin) {
        return origin.equals("https://to-do-list-mntn.vercel.app")
                || origin.equals("https://to-do-list-mntn-git-main-mouneesh.vercel.app")
                || origin.equals("https://to-do-list-mntn-b5dy2uhgf-mouneesh.vercel.app")
                || origin.startsWith("http://localhost")
                || origin.startsWith("http://127.0.0.1");
    }

    private void handleCorsPreflight(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }

    private void redirectHome(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/");
        exchange.sendResponseHeaders(303, -1);
    }
}
