package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import server.model.Test;
import server.model.TestStep;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TestManager {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static BrowserRecorder recorder;
    private static Long currentTestId; // Храним testId для текущего теста

    static void showCreateTestWindow() {
        Stage createTestStage = new Stage();
        createTestStage.setTitle("Создать новый тест");

        Label nameLabel = new Label("Название теста:");
        TextField nameInput = new TextField();
        nameInput.setId("nameInput");

        Label descriptionLabel = new Label("Описание теста:");
        TextField descriptionInput = new TextField();
        descriptionInput.setId("descriptionInput");

        Label urlLabel = new Label("URL теста:");
        TextField urlInput = new TextField();
        urlInput.setId("urlInput");

        Button saveButton = new Button("Сохранить");
        saveButton.setOnAction(e -> {
            String name = nameInput.getText();
            String description = descriptionInput.getText();
            String url = urlInput.getText();

            // Сохраняем тест и запоминаем testId
            try {
                String requestBody = String.format("{\"name\":\"%s\",\"description\":\"%s\",\"url\":\"%s\"}", name, description, url);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/tests/"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    Test createdTest = mapper.readValue(response.body(), Test.class);
                    currentTestId = createdTest.getId(); // Сохраняем testId
                    showTestWindow(name, description, url, FXCollections.observableArrayList());
                    createTestStage.close();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Ошибка при создании теста!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Не удалось сохранить тест.");
            }
        });

        VBox vbox = new VBox(10, nameLabel, nameInput, descriptionLabel, descriptionInput, urlLabel, urlInput, saveButton);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox, 400, 300);
        createTestStage.setScene(scene);
        createTestStage.show();
    }

    static void showOpenTestWindow() {
        Stage openTestStage = new Stage();
        openTestStage.setTitle("Открыть тест");

        TableView<Test> tableView = new TableView<>();

        TableColumn<Test, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Test, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Test, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Adding the URL column
        TableColumn<Test, String> urlColumn = new TableColumn<>("URL");
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));

        tableView.getColumns().addAll(idColumn, nameColumn, descriptionColumn, urlColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        ObservableList<Test> data = FXCollections.observableArrayList(fetchTests());
        tableView.setItems(data);

        Button deleteTestButton = new Button("Удалить тест");
        deleteTestButton.setOnAction(e -> {
            Test selectedTest = tableView.getSelectionModel().getSelectedItem();
            if (selectedTest != null) {
                try {
                    deleteTest(selectedTest.getId());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                data.remove(selectedTest);
            } else {
                showAlert(Alert.AlertType.WARNING, "Пожалуйста, выберите тест для удаления.");
            }
        });

        tableView.setRowFactory(tv -> {
            TableRow<Test> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Test rowData = row.getItem();
                    currentTestId = rowData.getId(); // Save the current testId
                    ObservableList<Step> steps = FXCollections.observableArrayList(fetchSteps(rowData.getId()));
                    showTestWindow(rowData.getName(), rowData.getDescription(), rowData.getUrl(), steps);
                    openTestStage.close();
                }
            });
            return row;
        });

        VBox layout = new VBox(tableView, deleteTestButton);
        Scene scene = new Scene(layout, 600, 400); // Adjusted size for the extra column
        openTestStage.setScene(scene);
        openTestStage.show();
    }

    private static void deleteTest(Long testId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/tests/" + testId)).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            throw new IOException("Failed to delete test: HTTP code " + response.statusCode());
        }
    }

    private static List<Test> fetchTests() {
        try {
            URL url = new URL("http://localhost:8080/api/tests/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            ObjectMapper mapper = new ObjectMapper();
            Test[] testsArray = mapper.readValue(connection.getInputStream(), Test[].class);
            return Arrays.asList(testsArray);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static List<Step> fetchSteps(Long testId) {
        try {
            URL url = new URL("http://localhost:8080/api/tests/" + testId + "/steps");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            ObjectMapper mapper = new ObjectMapper();
            TestStep[] stepsArray = mapper.readValue(connection.getInputStream(), TestStep[].class);

            List<Step> steps = new ArrayList<>();
            for (TestStep testStep : stepsArray) {
                steps.add(new Step(
                        testStep.getAction(),
                        testStep.getButton(),
                        testStep.getLocation(),
                        testStep.getDetails()
                ));
            }
            return steps;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static void showTestWindow(String name, String description, String url, ObservableList<Step> steps) {
        Stage testStage = new Stage();
        testStage.setTitle(name);

        Label nameLabel = new Label("Название теста:");
        TextField nameInput = new TextField(name);
        nameInput.setId("nameInput");

        Label descriptionLabel = new Label("Описание теста:");
        TextField descriptionInput = new TextField(description);
        descriptionInput.setId("descriptionInput");

        Label urlLabel = new Label("URL:");
        TextField urlInput = new TextField(url);
        urlInput.setId("urlInput");

        TableView<Step> stepsTable = new TableView<>(steps);

        TableColumn<Step, String> actionColumn = new TableColumn<>("Action");
        actionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));

        TableColumn<Step, String> buttonColumn = new TableColumn<>("Button");
        buttonColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getButton()));

        TableColumn<Step, String> locationColumn = new TableColumn<>("Location");
        locationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));

        TableColumn<Step, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetails()));

        stepsTable.getColumns().addAll(actionColumn, buttonColumn, locationColumn, detailsColumn);
        stepsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button startButton = new Button("Start Recording");
        Button stopButton = new Button("Stop Recording");
        stopButton.setDisable(true);
        Button replayButton = new Button("Replay Test");
        Button showReportButton = new Button("Show Report");
        showReportButton.setDisable(true);
        Button saveStepsButton = new Button("Save Steps");
        saveStepsButton.setDisable(true);
        Button updateButton = new Button("Update Test");
        if (steps.isEmpty()) {
            replayButton.setDisable(true);

        }
        startButton.setOnAction(e -> {
            String startUrl = urlInput.getText();
            if (startUrl == null || startUrl.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please enter a valid URL.");
                return;
            }
            System.out.println("URL is = " + startUrl);
            recorder = new BrowserRecorder();
            recorder.startRecording(startUrl);
            startButton.setDisable(true);
            stopButton.setDisable(false);
            replayButton.setDisable(true);
            showReportButton.setDisable(true);
            steps.clear();
        });
        stopButton.setOnAction(e -> {
            if (recorder != null) {
                recorder.stopRecording();
                try {
                    recorder.saveStepsToFile("steps.json");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                steps.addAll(recorder.getSteps());
                startButton.setDisable(false);
                stopButton.setDisable(true);
                replayButton.setDisable(false);
                saveStepsButton.setDisable(false);
            }
        });
        replayButton.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String replayUrl = urlInput.getText();
                    String mavenPath = "C:\\Users\\79673\\Downloads\\apache-maven-3.9.9-bin\\apache-maven-3.9.9\\bin\\mvn.cmd";
                    ProcessBuilder testPb = new ProcessBuilder(mavenPath, "clean", "test", "-Dtest=PlayerTest", "-Dtest.url=" + replayUrl, "-Dtest.testId=" + currentTestId);
                    testPb.inheritIO();
                    Process testProcess = testPb.start();
                    int testExitCode = testProcess.waitFor();
                    if (testExitCode != 0) {
                        throw new RuntimeException("Test execution failed.");
                    }
                    showReportButton.setOnAction(event -> {
                        ReportGenerator reportGenerator = new ReportGenerator();
                        reportGenerator.generateReport();
                        try {
                            reportGenerator.openReportInBrowser();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    showReportButton.setDisable(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
        saveStepsButton.setOnAction(e -> {
            try {
                Long testId = currentTestId; // Метод для получения текущего ID теста

                deleteExistingSteps(testId);
                List<TestStep> updatedSteps = new ArrayList<>();

                for (Step step : steps) {
                    TestStep testStep = new TestStep();
                    testStep.setTestId(testId);
                    testStep.setStepNumber(steps.indexOf(step) + 1); // Нумерация шагов
                    testStep.setAction(step.getAction());
                    testStep.setButton(step.getButton());
                    testStep.setLocation(step.getLocation());
                    testStep.setDetails(step.getDetails());
                    updatedSteps.add(testStep);

                    // Отправка POST-запроса для каждого шага
                    try {
                        sendPostRequest(testId.intValue(), testStep); // Передаём testId и текущий шаг
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Failed to save step " + testStep.getStepNumber() + ": " + ex.getMessage());
                        return; // Прекращаем выполнение, если запрос не удался
                    }
                }

                // Сообщение об успешном завершении
                showAlert(Alert.AlertType.INFORMATION, "All steps saved successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Failed to save steps: " + ex.getMessage());
            }
        });
        updateButton.setOnAction(e -> {
            String updatedName = nameInput.getText();
            String updatedDescription = descriptionInput.getText();
            String updatedUrl = urlInput.getText();
            Test updatedTest = new Test();
            updatedTest.setId(currentTestId);
            updatedTest.setName(updatedName);
            updatedTest.setDescription(updatedDescription);
            updatedTest.setUrl(updatedUrl);
            try {
                updateTest(currentTestId, updatedTest);
                showAlert(Alert.AlertType.INFORMATION, "Test updated successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Failed to update test: " + ex.getMessage());
            }
        });
        VBox vbox = new VBox(10, nameLabel, nameInput, descriptionLabel, descriptionInput, urlLabel, urlInput, updateButton, stepsTable, startButton, stopButton, replayButton, showReportButton, saveStepsButton);
        vbox.setPadding(new Insets(10));
        Scene scene = new Scene(vbox, 800, 600);
        testStage.setScene(scene);
        testStage.show();
    }

    private static void updateTest(Long testId, Test updatetTest) throws IOException, InterruptedException {
        String requestBody = String.format("{\"name\":\"%s\",\"description\":\"%s\",\"url\":\"%s\"}", updatetTest.getName(), updatetTest.getDescription(), updatetTest.getUrl());
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/tests/" + testId)).header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(requestBody)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to update test: HTTP code " + response.statusCode());
        }
    }

    private static void sendPostRequest(int testId, TestStep step) throws IOException {
        // Формируем URL для POST-запроса
        String url = "http://localhost:8080/api/tests/" + testId + "/steps";

        // Устанавливаем соединение
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Преобразование объекта TestStep в JSON
        String jsonInputString = new ObjectMapper().writeValueAsString(step);

        // Отправка данных в body запроса
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Проверка ответа
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new IOException("Failed to save step: HTTP code " + responseCode);
        }
    }

    private static void deleteExistingSteps(Long testId) throws IOException {
        // Формируем URL для DELETE-запроса
        String url = "http://localhost:8080/api/tests/" + testId + "/steps";

        // Устанавливаем соединение
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("DELETE");

        // Проверка ответа
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            throw new IOException("Failed to delete steps: HTTP code " + responseCode);
        }
    }


    private static void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

