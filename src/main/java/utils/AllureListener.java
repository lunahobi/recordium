package utils;

import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import static com.codeborne.selenide.Selenide.*;

@ExtendWith(AllureListener.class)
public class AllureListener implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        addListener();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        addListener();
        takeScreenshot("Failure screenshot");
    }

    private void addListener() {
        AllureSelenide allureSelenide = new AllureSelenide()
                .screenshots(true)
                .savePageSource(false);

        com.codeborne.selenide.logevents.SelenideLogger.addListener("AllureSelenide", allureSelenide);
    }

    private void takeScreenshot(String name) {
        screenshot(name);
    }
}
