package client;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.qameta.allure.Step;
import server.model.TestStep;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class PlayerTest {
    public static String url;
    WebDriver driver;
    private WebDriverWait wait;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    public Long testId;

    public void setUp() {
        WebDriverManager.chromedriver().setup();

        Configuration.browser = "chrome";
        Configuration.webdriverLogsEnabled = true;
        Configuration.headless = false;
        Configuration.timeout = 10000;

        // Получение URL из системного свойства
        url = System.getProperty("test.url", "https://reqres.in");
        testId = Long.valueOf(System.getProperty("test.testId", "1"));

        // Создание ChromeOptions
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-gpu");
        options.addArguments("--start-maximized");

        // Создание драйвера
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @BeforeEach
    public void init(){
        setUp();

        // Добавляем слушатель AllureSelenide
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide()
                .screenshots(true) // Скриншоты при ошибках
                .includeSelenideSteps(false)
                .savePageSource(false));
    }

    @AfterEach
    public void tearDown(){
        Selenide.closeWebDriver();
        driver.quit();
    }

    @Step("Open URL: {url}")
    public void openUrl(String url) {
        driver.get(url);
    }

    @Step("Replay step: {step.action} on {step.location} with details {step.details}")
    public void replayStep(TestStep step) {
        try {
            switch (step.getAction()) {
                case "Click":
                    WebElement element = findElementByXPath(step.getLocation());
                    scrollToElement(element);
                    wait.until(ExpectedConditions.elementToBeClickable(element)).click();
                    break;
                case "Input":
                    WebElement inputElement = findElementByXPath(step.getLocation());
                    scrollToElement(inputElement);
                    inputElement.sendKeys(step.getDetails());
                    break;
                // Add more action handling here
            }
        } catch (ElementClickInterceptedException e) {
            System.err.println("Element click intercepted: " + e.getMessage());
            throw e;
        } catch (TimeoutException e) {
            System.err.println("Element not found: " + step.getLocation());
            throw e;
        }
    }

    private WebElement findElementByXPath(String xpath) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
    }

    private void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public List<TestStep> fetchStepsFromApi(Long testId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/tests/" + testId + "/steps"))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch steps: HTTP code " + response.statusCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body(), new TypeReference<List<TestStep>>() {});
    }

    @Test
    public void test() throws IOException, InterruptedException {
        List<TestStep> steps = fetchStepsFromApi(testId);

        openUrl(url);
        for (TestStep step : steps) {
            try {
                replayStep(step);
            } catch (Exception e) {
                System.err.println("Error during step: " + step + " -> " + e.getMessage());
            }
        }
    }
}
