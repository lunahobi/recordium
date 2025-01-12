package tests;

import client.ReportGenerator;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.qameta.allure.Step;


import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class PlayerTest {
    public static String url = "https://reqres.in";
    WebDriver driver;
    private WebDriverWait wait;

    public void setUp() {
        WebDriverManager.chromedriver().setup();

        Configuration.browser = "chrome";
        Configuration.webdriverLogsEnabled = true;
        Configuration.headless = false;
        Configuration.timeout = 10000;

        // Создание ChromeOptions
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-gpu");
        options.addArguments("--start-maximized");

        // Создание драйвера
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Before
    public void init(){
        setUp();

        // Добавляем слушатель AllureSelenide
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide()
                .screenshots(true) // Скриншоты при ошибках
                .includeSelenideSteps(false)
                .savePageSource(false));
    }

    @After
    public void tearDown(){
        Selenide.closeWebDriver();
        driver.quit();
    }
    @Step("Open URL: {url}")
    public void openUrl(String url) {
        driver.get(url);
    }

    @Step("Replay step: {step.action} on {step.location} with details {step.details}")
    public void replayStep(client.Step step) {
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
    public void replayStepsFromFile(String fileName, String url) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new IOException("File not found: " + fileName);
        }
        ObjectMapper mapper = new ObjectMapper();
        List<client.Step> steps = mapper.readValue(file, new TypeReference<List<client.Step>>() {});
        openUrl(url);
        for (client.Step step : steps) {
            try {
                replayStep(step);
            } catch (Exception e) {
                System.err.println("Error during step: " + step + " -> " + e.getMessage());
            }
        }
    }

    @Test
    public void test() throws IOException {
        replayStepsFromFile("steps.json", url);

        // Генерация отчета Allure
        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.generateReport();

    }
}
