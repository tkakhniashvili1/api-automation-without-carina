package com.solvd.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class ApiClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String graphqlUrl;
    private final ObjectMapper objectMapper;

    public ApiClient(String baseUrl, String graphqlUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.baseUrl = baseUrl;
        this.graphqlUrl = graphqlUrl;
        this.objectMapper = new ObjectMapper();
    }

    public HttpResponse<String> send(String method, String path, String bearerToken, String body)
            throws IOException, InterruptedException {
        return send(method, path, bearerToken, body, Map.of());
    }

    public HttpResponse<String> send(String method,
                                     String path,
                                     String bearerToken,
                                     String body,
                                     Map<String, String> extraHeaders)
            throws IOException, InterruptedException {

        HttpRequest.BodyPublisher bodyPublisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }

        extraHeaders.forEach(requestBuilder::header);

        HttpRequest request = requestBuilder
                .method(method, bodyPublisher)
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> graphql(String bearerToken, String query)
            throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of("query", query));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(graphqlUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
