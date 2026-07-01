import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskWebServerTest {
    @Test
    void preflightRequestShouldBeAccepted() throws IOException, InterruptedException {
        TaskManager manager = new TaskManager();
        TaskWebServer server = new TaskWebServer(manager, 0);
        server.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/api/tasks"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://to-do-list-mntn.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertEquals("https://to-do-list-mntn.vercel.app", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }
}
