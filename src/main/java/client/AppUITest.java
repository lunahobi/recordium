package client;

import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.util.WaitForAsyncUtils;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class AppUITest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        new App().start(stage); // Запуск приложения
    }

    @Test
    public void testSaveNewTest(){
        // Открываем окно создания теста
        clickOn("Создать новый тест");
        WaitForAsyncUtils.waitForFxEvents();

        // Заполняем поля
        clickOn("#nameInput").write("Test Name");
        clickOn("#descriptionInput").write("Test Description");
        clickOn("#urlInput").write("http://example.com");

        // Нажимаем "Сохранить"
        clickOn("Сохранить");
        WaitForAsyncUtils.waitForFxEvents();

        // Проверяем, что окно теста открыто
        verifyThat("#nameInput", TextInputControlMatchers.hasText("Test Name"));
        verifyThat("#descriptionInput", TextInputControlMatchers.hasText("Test Description"));
        verifyThat("#urlInput", TextInputControlMatchers.hasText("http://example.com"));
    }

    @Test
    public void testOpenTestWindow(){
        // Нажимаем на кнопку "Открыть тест"
        clickOn("Открыть тест");
        WaitForAsyncUtils.waitForFxEvents();

        // Проверяем, что таблица отображается
        verifyThat(".table-view", tableView -> ((TableView<?>) tableView).getColumns().size() > 0);

        // Дважды кликаем на строку
        doubleClickOn(".table-row-cell");
        WaitForAsyncUtils.waitForFxEvents();

        // Проверяем, что открылось окно теста
        verifyThat(".label",hasText("Название теста:"));
    }

    @Test
    public void testUpdateTest(){
        // Открываем окно теста
        clickOn("Открыть тест");
        doubleClickOn(".table-row-cell"); // Открываем тест
        WaitForAsyncUtils.waitForFxEvents();

        // Обновляем поля
        clickOn("#nameInput").doubleClickOn().write("Updated Test Name");
        clickOn("#descriptionInput").doubleClickOn().write("Updated Test Description");
        clickOn("#urlInput").doubleClickOn().write("http://updated.com");

        // Нажимаем "Сохранить изменения"
        clickOn("Update Test");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(".dialog-pane", dialog ->
                ((DialogPane) dialog).getContentText().contains("Test updated successfully!")
        );

        // Проверяем, что данные теста обновились
        verifyThat("#nameInput", TextInputControlMatchers.hasText("Updated Test Name"));
        verifyThat("#descriptionInput", TextInputControlMatchers.hasText("Updated Test Description"));
        verifyThat("#urlInput", TextInputControlMatchers.hasText("http://updated.com"));
    }
}
