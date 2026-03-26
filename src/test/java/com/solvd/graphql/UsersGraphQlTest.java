package com.solvd.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.solvd.base.BaseApiTest;
import com.solvd.util.TestDataFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.http.HttpResponse;
import java.util.Map;

public class UsersGraphQlTest extends BaseApiTest {

    private HttpResponse<String> executeGraphQl(String templatePath, Map<String, String> placeholders) throws Exception {
        String query = render(templatePath, placeholders);
        return apiClient.graphql(token, query);
    }

    private long createUserViaGraphQl(String name, String email) throws Exception {
        HttpResponse<String> response = executeGraphQl("templates/graphql/create-user.graphql", Map.of(
                "NAME", name,
                "GENDER", "male",
                "EMAIL", email,
                "STATUS", "active"
        ));

        Assert.assertEquals(response.statusCode(), 200, response.body());
        JsonNode json = objectMapper.readTree(response.body());
        return json.at("/data/createUser/user/id").asLong();
    }

    private void deleteUserViaGraphQlQuietly(long id) {
        try {
            executeGraphQl("templates/graphql/delete-user.graphql", Map.of(
                    "ID", String.valueOf(id)
            ));
        } catch (Exception ignored) {
        }
    }

    @Test
    public void verifyCreateUserMutationReturns200AndCreatedUser() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();

        HttpResponse<String> response = executeGraphQl("templates/graphql/create-user.graphql", Map.of(
                "NAME", name,
                "GENDER", "male",
                "EMAIL", email,
                "STATUS", "active"
        ));

        Assert.assertEquals(response.statusCode(), 200, response.body());
        assertJsonContentType(response);

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode user = json.at("/data/createUser/user");

        Assert.assertTrue(user.has("id"), "Created user id is missing");
        Assert.assertEquals(user.get("name").asText(), name);
        Assert.assertEquals(user.get("email").asText(), email);
        Assert.assertEquals(user.get("gender").asText(), "male");
        Assert.assertEquals(user.get("status").asText(), "active");

        deleteUserViaGraphQlQuietly(user.get("id").asLong());
    }

    @Test
    public void verifyUserQueryReturns200AndRequestedUser() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUserViaGraphQl(name, email);

        try {
            HttpResponse<String> response = executeGraphQl("templates/graphql/get-user.graphql", Map.of(
                    "ID", String.valueOf(userId)
            ));

            Assert.assertEquals(response.statusCode(), 200, response.body());
            assertJsonContentType(response);

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode user = json.at("/data/user");

            Assert.assertEquals(user.get("id").asLong(), userId);
            Assert.assertEquals(user.get("name").asText(), name);
            Assert.assertEquals(user.get("email").asText(), email);
        } finally {
            deleteUserViaGraphQlQuietly(userId);
        }
    }

    @Test
    public void verifyUpdateUserMutationReturns200AndUpdatedUser() throws Exception {
        String originalName = TestDataFactory.uniqueName();
        String originalEmail = TestDataFactory.uniqueEmail();
        long userId = createUserViaGraphQl(originalName, originalEmail);

        try {
            String updatedName = TestDataFactory.uniqueName();
            String updatedEmail = TestDataFactory.uniqueEmail();

            HttpResponse<String> response = executeGraphQl("templates/graphql/update-user.graphql", Map.of(
                    "ID", String.valueOf(userId),
                    "NAME", updatedName,
                    "EMAIL", updatedEmail,
                    "GENDER", "female",
                    "STATUS", "inactive"
            ));

            Assert.assertEquals(response.statusCode(), 200, response.body());
            assertJsonContentType(response);

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode user = json.at("/data/updateUser/user");

            Assert.assertEquals(user.get("id").asLong(), userId);
            Assert.assertEquals(user.get("name").asText(), updatedName);
            Assert.assertEquals(user.get("email").asText(), updatedEmail);
            Assert.assertEquals(user.get("gender").asText(), "female");
            Assert.assertEquals(user.get("status").asText(), "inactive");
        } finally {
            deleteUserViaGraphQlQuietly(userId);
        }
    }

    @Test
    public void verifyDeleteUserMutationReturns200AndDeletedUser() throws Exception {
        String name = TestDataFactory.uniqueName();
        String email = TestDataFactory.uniqueEmail();
        long userId = createUserViaGraphQl(name, email);

        HttpResponse<String> response = executeGraphQl("templates/graphql/delete-user.graphql", Map.of(
                "ID", String.valueOf(userId)
        ));

        Assert.assertEquals(response.statusCode(), 200, response.body());
        assertJsonContentType(response);

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode user = json.at("/data/deleteUser/user");

        Assert.assertEquals(user.get("id").asLong(), userId);
        Assert.assertEquals(user.get("name").asText(), name);
        Assert.assertEquals(user.get("email").asText(), email);
    }

    @Test
    public void verifyInvalidGraphQlQueryReturnsErrors() throws Exception {
        HttpResponse<String> response = executeGraphQl("templates/graphql/invalid-user-field.graphql", Map.of());

        Assert.assertTrue(
                response.statusCode() == 200 || response.statusCode() == 400,
                "Unexpected GraphQL error status: " + response.statusCode() + ", body: " + response.body()
        );
        assertJsonContentType(response);

        JsonNode json = objectMapper.readTree(response.body());
        Assert.assertTrue(json.has("errors"), "GraphQL errors field is missing");
        Assert.assertTrue(json.get("errors").isArray(), "GraphQL errors must be an array");
        Assert.assertTrue(json.get("errors").size() > 0, "GraphQL errors array must not be empty");
    }
}
