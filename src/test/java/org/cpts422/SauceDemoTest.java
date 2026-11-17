package org.cpts422;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SauceDemoTest {
    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeEach
    void openLoginPage() {
        context = browser.newContext();
        page = context.newPage();
        page.navigate("https://www.saucedemo.com/");
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    private void loginAs(String username, String password) {
        // Use user-facing accessibility labels (not CSS) so the test behaves like a real user.
        page.getByLabel("Username").fill(username);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
    }

    @Test
    @DisplayName("T1: Successful purchase of two items")
    void t1SuccessfulPurchaseOfTwoItems() {
        loginAs("standard_user", "secret_sauce");
    }

    @AfterAll
    static void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
        } finally {
            if (playwright != null) {
                playwright.close();
            }
        }
    }
}
