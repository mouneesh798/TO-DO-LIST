import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskWebServer {
    private final TaskManager manager;
    private final int port;

    public TaskWebServer(TaskManager manager) {
        this(manager, 8080);
    }

    public TaskWebServer(TaskManager manager, int port) {
        this.manager = manager;
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", this::handleRoot);
        server.createContext("/tasks", this::handleTasks);
        server.createContext("/tasks/complete", this::handleCompleteTask);
        server.createContext("/tasks/delete", this::handleDeleteTask);
        server.setExecutor(null);
        server.start();
    }

    public int getPort() {
        return port;
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
            String taskName = parseTaskName(formData);
            manager.addTask(taskName);
            redirectHome(exchange);
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

    private void handleCompleteTask(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            redirectHome(exchange);
            return;
        }

        String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        int taskIndex = parseTaskIndex(formData);
        if (taskIndex > 0) {
            manager.markCompleted(taskIndex);
        }
        redirectHome(exchange);
    }

    private void handleDeleteTask(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            redirectHome(exchange);
            return;
        }

        String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        int taskIndex = parseTaskIndex(formData);
        if (taskIndex > 0) {
            manager.deleteTask(taskIndex);
        }
        redirectHome(exchange);
    }

    private String buildHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Java To-Do List</title>");
        html.append("<style>");
        html.append("*{box-sizing:border-box;}");
        html.append(":root{color-scheme:light;--ink:#172033;--muted:#6f7a8e;--line:#dde5f2;--panel:#ffffff;--accent:#0f8f87;--accent-dark:#0b6f69;--gold:#f6b73c;--danger:#e75252;}");
        html.append("body{margin:0;min-height:100vh;font-family:Inter,Segoe UI,Roboto,Arial,sans-serif;color:var(--ink);background:linear-gradient(135deg,#f5f8ff 0%,#eef7f4 45%,#fff7e6 100%);}");
        html.append("body:before{content:'';position:fixed;inset:0;pointer-events:none;background:radial-gradient(circle at 16% 12%,rgba(15,143,135,.18),transparent 30%),radial-gradient(circle at 82% 18%,rgba(246,183,60,.22),transparent 28%);}");
        html.append(".page{position:relative;width:min(1040px,calc(100% - 32px));margin:0 auto;padding:48px 0;}");
        html.append(".hero{display:grid;grid-template-columns:minmax(0,1.1fr) 320px;gap:28px;align-items:stretch;margin-bottom:26px;}");
        html.append(".hero-main,.stats,.tasks{background:rgba(255,255,255,.82);border:1px solid rgba(221,229,242,.9);box-shadow:0 24px 70px rgba(51,72,104,.14);backdrop-filter:blur(16px);}");
        html.append(".hero-main{border-radius:8px;padding:34px;display:flex;flex-direction:column;justify-content:space-between;min-height:300px;}");
        html.append(".eyebrow{margin:0 0 14px;font-size:12px;font-weight:800;letter-spacing:0;text-transform:uppercase;color:var(--accent);}");
        html.append("h1{margin:0;max-width:720px;font-size:clamp(36px,7vw,74px);line-height:.95;letter-spacing:0;}");
        html.append(".subtitle{margin:18px 0 0;max-width:620px;color:var(--muted);font-size:18px;line-height:1.6;}");
        html.append(".add-form{display:flex;gap:12px;margin-top:32px;padding:8px;background:#f6f9fd;border:1px solid var(--line);border-radius:8px;}");
        html.append("input{min-width:0;flex:1;border:0;background:transparent;padding:14px 14px;font-size:16px;color:var(--ink);outline:none;}");
        html.append("input::placeholder{color:#9aa6b8;}");
        html.append("button{border:0;border-radius:6px;padding:0 22px;font-weight:800;font-size:15px;color:#fff;background:linear-gradient(135deg,var(--accent),var(--accent-dark));box-shadow:0 12px 24px rgba(15,143,135,.26);cursor:pointer;transition:transform .18s ease,box-shadow .18s ease;white-space:nowrap;}");
        html.append("button:hover{transform:translateY(-1px);box-shadow:0 16px 30px rgba(15,143,135,.32);}");
        html.append(".stats{border-radius:8px;padding:26px;display:grid;gap:16px;align-content:space-between;}");
        html.append(".stat{padding:18px;border-radius:8px;background:#f8fbff;border:1px solid var(--line);}");
        html.append(".stat strong{display:block;font-size:36px;line-height:1;margin-bottom:8px;}");
        html.append(".stat span{color:var(--muted);font-size:14px;font-weight:700;}");
        html.append(".tasks{border-radius:8px;padding:26px;}");
        html.append(".section-title{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:20px;}");
        html.append("h2{margin:0;font-size:24px;letter-spacing:0;}");
        html.append(".pill{display:inline-flex;align-items:center;min-height:30px;padding:0 12px;border-radius:999px;background:#edf7f6;color:var(--accent-dark);font-size:13px;font-weight:800;}");
        html.append("ul{list-style:none;margin:0;padding:0;display:grid;gap:12px;}");
        html.append("li{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:center;gap:14px;padding:16px;border:1px solid var(--line);border-radius:8px;background:#fff;box-shadow:0 10px 30px rgba(51,72,104,.07);}");
        html.append(".check{display:grid;place-items:center;flex:0 0 34px;width:34px;height:34px;border-radius:50%;font-weight:900;color:#fff;background:var(--gold);}");
        html.append(".task-text{min-width:0;overflow-wrap:anywhere;font-size:16px;font-weight:700;}");
        html.append(".task-actions{display:flex;gap:8px;margin:0;padding:0;background:transparent;border:0;}");
        html.append(".task-actions button{min-height:38px;padding:0 14px;font-size:13px;box-shadow:none;}");
        html.append(".done-btn{background:linear-gradient(135deg,var(--accent),var(--accent-dark));}");
        html.append(".remove-btn{background:linear-gradient(135deg,var(--danger),#b82f2f);}");
        html.append(".complete .check{background:var(--accent);}");
        html.append(".complete .task-text{color:#7a8497;text-decoration:line-through;}");
        html.append(".empty{padding:32px;border:1px dashed #bcc8d9;border-radius:8px;text-align:center;color:var(--muted);background:#fbfdff;font-weight:700;}");
        html.append("@media(max-width:760px){.page{width:min(100% - 24px,1040px);padding:24px 0;}.hero{grid-template-columns:1fr;}.hero-main{padding:24px;min-height:auto;}.add-form{flex-direction:column;}button{min-height:48px;}.section-title{align-items:flex-start;flex-direction:column;}.stats{grid-template-columns:1fr 1fr;}}");
        html.append("@media(max-width:560px){li{grid-template-columns:34px minmax(0,1fr);align-items:start;}.task-actions{grid-column:2;}.task-actions button{min-height:40px;}}");
        html.append("@media(max-width:460px){.stats{grid-template-columns:1fr;}.check{margin-top:2px;}}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        List<Task> tasks = manager.getTasks();
        long completedCount = tasks.stream().filter(Task::isCompleted).count();
        int pendingCount = tasks.size() - (int) completedCount;
        html.append("<main class='page'>");
        html.append("<section class='hero'>");
        html.append("<div class='hero-main'>");
        html.append("<div>");
        html.append("<p class='eyebrow'>Focused task manager</p>");
        html.append("<h1>Plan the day with clarity.</h1>");
        html.append("<p class='subtitle'>A cleaner Java to-do dashboard for capturing tasks quickly and keeping your current workload easy to scan.</p>");
        html.append("</div>");
        html.append("<form class='add-form' method='post' action='/tasks'>");
        html.append("<input name='task' placeholder='Add a new task...' autocomplete='off' required />");
        html.append("<button type='submit'>Add Task</button>");
        html.append("</form>");
        html.append("</div>");
        html.append("<aside class='stats' aria-label='Task summary'>");
        html.append("<div class='stat'><strong>").append(tasks.size()).append("</strong><span>Total Tasks</span></div>");
        html.append("<div class='stat'><strong>").append(pendingCount).append("</strong><span>Pending</span></div>");
        html.append("<div class='stat'><strong>").append(completedCount).append("</strong><span>Completed</span></div>");
        html.append("</aside>");
        html.append("</section>");
        html.append("<section class='tasks'>");
        html.append("<div class='section-title'><h2>Current Tasks</h2><span class='pill'>").append(pendingCount).append(" still open</span></div>");
        if (tasks.isEmpty()) {
            html.append("<div class='empty'>No tasks yet. Add one above to start your list.</div>");
        } else {
            html.append("<ul>");
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                int taskNumber = i + 1;
                html.append("<li class='").append(task.isCompleted() ? "complete" : "pending").append("'>");
                html.append("<span class='check'>").append(task.isCompleted() ? "&#10003;" : "&bull;").append("</span>");
                html.append("<span class='task-text'>").append(escapeHtml(task.getTaskName())).append("</span>");
                if (task.isCompleted()) {
                    html.append("<form class='task-actions' method='post' action='/tasks/delete'>");
                    html.append("<input type='hidden' name='index' value='").append(taskNumber).append("'>");
                    html.append("<button class='remove-btn' type='submit'>Remove</button>");
                    html.append("</form>");
                } else {
                    html.append("<form class='task-actions' method='post' action='/tasks/complete'>");
                    html.append("<input type='hidden' name='index' value='").append(taskNumber).append("'>");
                    html.append("<button class='done-btn' type='submit'>Done</button>");
                    html.append("</form>");
                }
                html.append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>");
        html.append("</main>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String parseTaskName(String formData) {
        for (String pair : formData.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "task".equals(parts[0])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private int parseTaskIndex(String formData) {
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

    private void redirectHome(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/");
        exchange.sendResponseHeaders(303, -1);
    }
}
