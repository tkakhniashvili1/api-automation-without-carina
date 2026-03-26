package com.solvd.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvd.client.ApiClient;
import com.solvd.config.Config;
import com.solvd.util.ResourceUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public abstract class BaseApiTest {
    protected ApiClient apiClient;
    protected ObjectMapper objectMapper;
    protected String token;

    @BeforeClass(alwaysRun = true)
    public void setUpBase() {
        apiClient = new ApiClient(
                Config.getRequired("api.base.url"),
                Config.getRequired("api.graphql.url")
        );
        objectMapper = new ObjectMapper();
        token = Config.getRequired("gorest.token");
    }

    protected String render(String resourcePath, Map<String, String> placeholders) {
        return ResourceUtils.renderTemplate(resourcePath, placeholders);
    }

    protected void assertJsonContentType(HttpResponse<String> response) {
        String contentType = response.headers().firstValue("content-type").orElse("");
        Assert.assertTrue(
                contentType.contains("application/json"),
                "Unexpected content-type: " + contentType
        );
    }

    protected Optional<String> header(HttpResponse<String> response, String name) {
        return response.headers().firstValue(name);
    }
}
