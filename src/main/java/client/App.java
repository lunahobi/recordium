package client;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Recordium - Test Recorder");

        Button createTestButton = new Button("Создать новый тест");
        Button openTestButton = new Button("Открыть тест");

        createTestButton.setOnAction(e -> TestManager.showCreateTestWindow());
        openTestButton.setOnAction(e -> TestManager.showOpenTestWindow());

        VBox layout = new VBox(10, createTestButton, openTestButton);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
