package com.solvd.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.solvd.base.BaseApiTest;
import com.solvd.util.TestDataFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UsersRestTest extends BaseApiTest {

    private long createUser(String name, String email) throws Exception {
        String body = render("templates/rest/create-user.json", Map.of(
                "NAME", name,
                "GENDER", "male",
                "EMAIL", email,
                "STATUS", "active"
        ));

        HttpResponse<String> response = apiClient.send("POST", "/users", token, body);
        Assert.assertEquals(response.statusCode(), 201, response.body());

        JsonNode json = objectMapper.readTree(response.body());
        return json.get("id").asLong();
    }

    private void deleteUserQuietly(long id) {
        try {
            apiClient.send("DELETE", "/users/" + id, token, null);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void verifyCreateUserReturns201AndLocationHeader() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();

        String body = render("templates/rest/create-user.json", Map.of(
                "NAME", name,
                "GENDER", "male",
                "EMAIL", email,
                "STATUS", "active"
        ));

        HttpResponse<String> response = apiClient.send("POST", "/users", token, body);

        Assert.assertEquals(response.statusCode(), 201, response.body());
        assertJsonContentType(response);
        Assert.assertTrue(header(response, "location").isPresent(), "Location header is missing");

        JsonNode json = objectMapper.readTree(response.body());
        Assert.assertEquals(json.get("name").asText(), name);
        Assert.assertEquals(json.get("email").asText(), email);
        Assert.assertEquals(json.get("gender").asText(), "male");
        Assert.assertEquals(json.get("status").asText(), "active");

        deleteUserQuietly(json.get("id").asLong());
    }

    @Test
    public void verifyGetCreatedUserByIdReturns200() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUser(name, email);

        try {
            HttpResponse<String> response = apiClient.send("GET", "/users/" + userId, token, null);

            Assert.assertEquals(response.statusCode(), 200, response.body());
            assertJsonContentType(response);
            Assert.assertTrue(header(response, "etag").isPresent(), "ETag header is missing");

            JsonNode json = objectMapper.readTree(response.body());
            Assert.assertEquals(json.get("id").asLong(), userId);
            Assert.assertEquals(json.get("name").asText(), name);
            Assert.assertEquals(json.get("email").asText(), email);
        } finally {
            deleteUserQuietly(userId);
        }
    }

    @Test
    public void verifySearchCreatedUserByEmailReturns200AndPaginationHeaders() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUser(name, email);

        try {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            HttpResponse<String> response = apiClient.send("GET", "/users?email=" + encodedEmail, token, null);

            Assert.assertEquals(response.statusCode(), 200, response.body());
            assertJsonContentType(response);
            Assert.assertTrue(header(response, "x-pagination-page").isPresent(), "x-pagination-page header is missing");
            Assert.assertTrue(header(response, "x-pagination-limit").isPresent(), "x-pagination-limit header is missing");

            JsonNode json = objectMapper.readTree(response.body());
            Assert.assertTrue(json.isArray(), "Response is not an array");

            boolean found = false;
            for (JsonNode user : json) {
                if (email.equals(user.get("email").asText()) && userId == user.get("id").asLong()) {
                    found = true;
                    break;
                }
            }

            Assert.assertTrue(found, "Created user was not found in filtered list");
        } finally {
            deleteUserQuietly(userId);
        }
    }

    @Test
    public void verifyPutUserReturns200AndReplacesFields() throws Exception {
        String originalName = TestDataFactory.uniqueName();
        String originalEmail = TestDataFactory.uniqueEmail();
        long userId = createUser(originalName, originalEmail);

        try {
            String updatedName = TestDataFactory.uniqueName();
            String updatedEmail = TestDataFactory.uniqueEmail();

            String body = render("templates/rest/update-user-put.json", Map.of(
                    "NAME", updatedName,
                    "GENDER", "female",
                    "EMAIL", updatedEmail,
                    "STATUS", "inactive"
            ));

            HttpResponse<String> response = apiClient.send("PUT", "/users/" + userId, token, body);

            Assert.assertEquals(response.statusCode(), 200, response.body());
            assertJsonContentType(response);

            JsonNode json = objectMapper.readTree(response.body());
            Assert.assertEquals(json.get("id").asLong(), userId);
            Assert.assertEquals(json.get("name").asText(), updatedName);
            Assert.assertEquals(json.get("email").asText(), updatedEmail);
            Assert.assertEquals(json.get("gender").asText(), "female");
            Assert.assertEquals(json.get("status").asText(), "inactive");
        } finally {
            deleteUserQuietly(userId);
        }
    }

    @Test
    public void verifyPatchUserReturns200AndUpdatesOnlyProvidedFields() throws Exception {
        String originalName = TestDataFactory.uniqueName();
        String originalEmail = TestDataFactory.uniqueEmail();
        long userId = createUser(originalName, originalEmail);

        try {
            String patchedName = TestDataFactory.uniqueName();

            String body = render("templates/rest/update-user-patch.json", Map.of(
                    "NAME", patchedName,
                    "STATUS", "inactive"
            ));

            HttpResponse<String> response = apiClient.send("PATCH", "/users/" + userId, token, body);

            Assert.assertEquals(response.statusCode(), 200, response.body());
            assertJsonContentType(response);

            JsonNode json = objectMapper.readTree(response.body());
            Assert.assertEquals(json.get("id").asLong(), userId);
            Assert.assertEquals(json.get("name").asText(), patchedName);
            Assert.assertEquals(json.get("email").asText(), originalEmail);
            Assert.assertEquals(json.get("status").asText(), "inactive");
        } finally {
            deleteUserQuietly(userId);
        }
    }

    @Test
    public void verifyDeleteUserReturns204AndEmptyBody() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUser(name, email);

        HttpResponse<String> response = apiClient.send("DELETE", "/users/" + userId, token, null);

        Assert.assertEquals(response.statusCode(), 204, response.body());
        Assert.assertTrue(response.body().isBlank(), "Delete response body must be empty");
    }

    @Test
    public void verifyDeletedUserReturns404() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUser(name, email);

        HttpResponse<String> deleteResponse = apiClient.send("DELETE", "/users/" + userId, token, null);
        Assert.assertEquals(deleteResponse.statusCode(), 204, deleteResponse.body());

        HttpResponse<String> getResponse = apiClient.send("GET", "/users/" + userId, token, null);

        Assert.assertEquals(getResponse.statusCode(), 404, getResponse.body());
        assertJsonContentType(getResponse);
        Assert.assertTrue(getResponse.body().toLowerCase().contains("not found"), getResponse.body());
    }

    @Test
    public void verifyCreateUserWithInvalidDataReturns422() throws Exception {
        String body = render("templates/rest/invalid-user.json", Map.of(
                "NAME", TestDataFactory.uniqueName(),
                "STATUS", "active"
        ));

        HttpResponse<String> response = apiClient.send("POST", "/users", token, body);

        Assert.assertEquals(response.statusCode(), 422, response.body());
        assertJsonContentType(response);

        JsonNode json = objectMapper.readTree(response.body());
        Assert.assertTrue(json.isArray(), "Validation errors response must be an array");
        Assert.assertTrue(json.size() > 0, "Validation errors response must not be empty");
    }

    @Test
    public void verifyHeadUserReturns404ForUnsupportedMethod() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUser(name, email);

        try {
            HttpResponse<String> response = apiClient.send("HEAD", "/users/" + userId, token, null);

            Assert.assertEquals(response.statusCode(), 404, response.body());
            Assert.assertTrue(response.body().isBlank(), "HEAD response body must be empty");
        } finally {
            deleteUserQuietly(userId);
        }
    }

    @Test
    public void verifyOptionsUsersEndpointReturns404ForUnsupportedMethod() throws Exception {
        HttpResponse<String> response = apiClient.send(
                "OPTIONS",
                "/users",
                token,
                null,
                Map.of("Origin", "http://localhost:3000")
        );

        Assert.assertEquals(response.statusCode(), 404, response.body());
        assertJsonContentType(response);

        JsonNode json = objectMapper.readTree(response.body());
        Assert.assertEquals(json.get("status").asInt(), 404);
        Assert.assertEquals(json.get("error").asText(), "Not Found");
    }
}