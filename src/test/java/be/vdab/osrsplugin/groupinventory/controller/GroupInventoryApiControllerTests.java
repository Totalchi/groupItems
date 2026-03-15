package be.vdab.osrsplugin.groupinventory.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GroupInventoryApiControllerTests {
    @LocalServerPort
    private int port;

    @Test
    void apiStoresOverviewAndRendersGroupPage() throws Exception {
        var httpClient = HttpClient.newHttpClient();
        var createResponse = send(httpClient, "POST", "/api/groups", null, """
                {
                  "groupName": "Web Team"
                }
                """);
        assertThat(createResponse.statusCode()).isEqualTo(200);
        assertThat(createResponse.body()).contains("\"groupName\":\"Web Team\"");

        var groupCode = createResponse.body().replaceAll(".*\"groupCode\":\"([^\"]+)\".*", "$1");

        var uploadResponse = sendJson(httpClient, "PUT", "/api/group-inventory/members/Alice", groupCode, """
                {
                  "items": [
                    {"itemName": "Dragon scimitar", "quantity": 1},
                    {"itemName": "Prayer potion(4)", "quantity": 7}
                  ]
                }
                """);
        assertThat(uploadResponse.statusCode()).isEqualTo(200);

        var targetResponse = sendJson(httpClient, "PUT", "/api/group-inventory/targets", groupCode, """
                {
                  "items": [
                    {"itemName": "Prayer potion(4)", "quantity": 10}
                  ]
                }
                """);
        assertThat(targetResponse.statusCode()).isEqualTo(200);

        var overviewResponse = send(httpClient, "GET", "/api/group-inventory", groupCode, null);
        assertThat(overviewResponse.statusCode()).isEqualTo(200);
        assertThat(overviewResponse.body()).contains("\"groupName\":\"Web Team\"");
        assertThat(overviewResponse.body()).contains("\"memberCount\":1");
        assertThat(overviewResponse.body()).contains("\"memberName\":\"Alice\"");
        assertThat(overviewResponse.body()).contains("\"missingQuantity\":3");

        var pageResponse = send(httpClient, "GET", "/groups/" + groupCode, null, null);
        assertThat(pageResponse.statusCode()).isEqualTo(200);
        assertThat(pageResponse.body()).contains("Web Team", groupCode, "Alice", "Dragon scimitar", "Prayer potion(4)");
    }

    private HttpResponse<String> sendJson(HttpClient client, String method, String path, String groupCode, String body) throws Exception {
        return send(client, method, path, groupCode, body);
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String groupCode, String body) throws Exception {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (groupCode != null) {
            requestBuilder.header("X-Group-Code", groupCode);
        }
        if (body != null) {
            requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
