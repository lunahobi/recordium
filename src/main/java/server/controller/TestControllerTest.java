package server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import server.model.TestStep;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static Long testId;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://localhost:8080/api/tests"; // Используем HTTP вместо HTTPS
    }

    @Test
    @Order(1)
    public void testCreateTest() throws Exception {
        server.model.Test test = new server.model.Test();
        test.setName("New Test");
        test.setDescription("Description");
        test.setUrl("http://example.com");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(test))
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .response();

        testId = response.jsonPath().getLong("id");

        response.then()
                .body("name", equalTo("New Test"))
                .body("description", equalTo("Description"))
                .body("url", equalTo("http://example.com"));
    }

    @Test
    @Order(2)
    public void testGetTests() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .body("find { it.id == " + testId + " }.name", equalTo("New Test"))
                .body("find { it.id == " + testId + " }.description", equalTo("Description"))
                .body("find { it.id == " + testId + " }.url", equalTo("http://example.com"));
    }

    @Test
    @Order(3)
    public void testUpdateTest() throws Exception {
        server.model.Test updatedTest = new server.model.Test();
        updatedTest.setName("Updated Test");
        updatedTest.setDescription("Updated Description");
        updatedTest.setUrl("http://updated.com");

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(updatedTest))
                .when()
                .put("/{id}", testId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Test"))
                .body("description", equalTo("Updated Description"))
                .body("url", equalTo("http://updated.com"));
    }

    @Test
    @Order(6)
    public void testDeleteTest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/{id}", testId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}", testId)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(4)
    public void testGetTestSteps() throws JsonProcessingException {
        TestStep step = new TestStep();
        step.setTestId(testId);
        step.setId(50L);
        step.setStepNumber(1);
        step.setAction("Click");
        step.setButton("Button");
        step.setLocation("id('element')");
        step.setDetails("Details");

        System.out.println(objectMapper.writeValueAsString(step));

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(step))
                .when()
                .post("/{id}/steps", testId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}/steps", testId)
                .then()
                .statusCode(200)
                .body("find { it.stepNumber == 1 }.action", equalTo("Click"))
                .body("find { it.stepNumber == 1 }.button", equalTo("Button"))
                .body("find { it.stepNumber == 1 }.location", equalTo("id('element')"))
                .body("find { it.stepNumber == 1 }.details", equalTo("Details"));
    }

    @Test
    @Order(5)
    public void testCreateTestStep() throws Exception {
        TestStep step = new TestStep();
        step.setTestId(testId);
        step.setId(50L);
        step.setStepNumber(2);
        step.setAction("Input");
        step.setButton("Button");
        step.setLocation("id('input')");
        step.setDetails("Some details");

        given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(step))
                .when()
                .post("/{id}/steps", testId)
                .then()
                .statusCode(200)
                .body("action", equalTo("Input"))
                .body("button", equalTo("Button"))
                .body("location", equalTo("id('input')"))
                .body("details", equalTo("Some details"));
    }
}
