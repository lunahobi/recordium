package client;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.codeborne.selenide.Selenide.open;

public class BrowserRecorder {
    private WebDriver driver;
    private List<Step> steps = new ArrayList<>();
    private DevTools devTools;
    private Map<String, String> lastInputs = new LinkedHashMap<>();

    public BrowserRecorder() {
        // Автоматическая настройка драйвера
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

        // Установка драйвера в Selenide
        WebDriverRunner.setWebDriver(driver);

        // Инициализация DevTools
        devTools = ((ChromeDriver) driver).getDevTools();
        devTools.createSession();
    }

    private void injectJavaScriptRecorder() {
        String script = """
                    if (!window.recordedEvents) {
                        window.recordedEvents = [];

                        function addEvent(action, button, location, details) {
                            window.recordedEvents.push({ action, button, location, details });
                        }

                        document.addEventListener('click', function(event) {
                            const element = event.target;
                            const xpath = getElementXPath(element);
                            if (xpath) {
                                const text = element.innerText || element.value || element.getAttribute('aria-label') || element.getAttribute('name') || element.id;
                                addEvent('Click', text, xpath, '');
                                saveLastInput(event);
                            }
                        });

                        document.addEventListener('blur', function(event) {
                            if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
                                const element = event.target;
                                const name = element.getAttribute('name') || element.id || 'unknown';
                                const value = element.value;
                                const xpath = getElementXPath(element);
                                if (xpath && value) {
                                    addEvent('Input', name, xpath, value);
                                }
                            }
                        }, true);

                        window.addEventListener('hashchange', function() {
                            addEvent('URL Change', '', '', window.location.href);
                        });

                        window.addEventListener('beforeunload', function() {
                            addEvent('Page Unload', '', '', window.location.href);
                        });

                        function getElementXPath(element) {
                            if (element.id !== '') {
                                return 'id("' + element.id + '")';
                            }
                            if (element === document.body) {
                                return '/html/' + element.tagName.toLowerCase();
                            }
                            let ix = 0;
                            const siblings = element.parentNode.childNodes;
                            for (let i = 0; i < siblings.length; i++) {
                                const sibling = siblings[i];
                                if (sibling === element) {
                                    return getElementXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + (ix + 1) + ']';
                                }
                                if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {
                                    ix++;
                                }
                            }
                            return null;
                        }

                        function saveLastInput(event) {
                            const element = event.target;
                            const name = element.getAttribute('name') || element.id || 'unknown';
                            const value = element.value;
                            window.recordedEvents.push({ action: 'Input', button: name, location: '', details: value });
                        }
                    }
                """;
        ((JavascriptExecutor) driver).executeScript(script);
        System.out.println("JavaScript recorder injected successfully.");
    }


    public void startRecording(String url) {
        try {
            open(url);
            injectJavaScriptRecorder();
            monitorEvents();
            System.out.println("Recording started for: " + url);

            // Ожидание открытия новых окон и вкладок
            new Thread(() -> {
                Set<String> existingHandles = driver.getWindowHandles();
                while (!driver.toString().contains("null")) {
                    try {
                        Set<String> windowHandles = driver.getWindowHandles();
                        if (windowHandles.size() > existingHandles.size()) {
                            for (String handle : windowHandles) {
                                if (!existingHandles.contains(handle)) {
                                    driver.switchTo().window(handle);
                                    injectJavaScriptRecorder();
                                    existingHandles = windowHandles; // Обновляем существующие дескрипторы окон
                                }
                            }
                        }
                        Thread.sleep(3000); // Проверка каждые 3 секунды
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error while opening URL: " + url);
        }
    }


    public List<Step> getSteps() {
        return steps;
    }

    private List<Step> getRecordedEvents() {
        try {
            List<Map<String, Object>> events = (List<Map<String, Object>>) ((JavascriptExecutor) driver)
                    .executeScript("var events = window.recordedEvents; window.recordedEvents = []; return events;");
            if (events == null) {
                return new ArrayList<>();
            }
            List<Step> steps = new ArrayList<>();
            for (Map<String, Object> event : events) {
                String action = String.valueOf(event.get("action"));
                String button = String.valueOf(event.get("button"));
                String location = String.valueOf(event.get("location"));
                String details = String.valueOf(event.get("details"));

                // Исключить дублирующиеся URL Change действия
                if (action.equals("URL Change")) {
                    if (!steps.isEmpty()) {
                        Step lastStep = steps.get(steps.size() - 1);
                        if (lastStep.getAction().equals("URL Change") && lastStep.getDetails().equals(details)) {
                            continue;
                        }
                    }
                }

                // Исключить пустые XPath для Input
                if (action.equals("Input") && (location == null || location.isEmpty())) {
                    continue;
                }

                Step step = new Step(action, button, location, details);
                steps.add(step);
            }
            return steps;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to retrieve recorded events.");
            return new ArrayList<>();
        }
    }

    public void monitorEvents() {
        new Thread(() -> {
            boolean recording = true;
            while (recording) {
                try {
                    if (driver == null || driver.toString().contains("null")) {
                        recording = false; // Завершение записи, если браузер закрыт
                    } else {
                        List<Step> events = getRecordedEvents();
                        if (!events.isEmpty()) {
                            for (Step event : events) {
                                if (!steps.contains(event)) {
                                    steps.add(event);
                                }
                            }
                            System.out.println("Updated steps: " + steps);
                        }
                    }
                    Thread.sleep(3000); // Проверка каждые 3 секунды
                } catch (InterruptedException e) {
                    recording = false; // Завершение записи при прерывании потока
                }
            }
            System.out.println("Event monitoring stopped.");
        }).start();
    }

    public void stopRecording() {
        if (driver != null) {
            // Получаем финальные события
            steps.addAll(getRecordedEvents());
            driver.quit();
            System.out.println("Recording stopped. Steps: " + steps);
        }
    }

    public void saveStepsToFile(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FileWriter fileWriter = new FileWriter(fileName);
        mapper.writeValue(fileWriter, steps);
        fileWriter.close();
    }


}
