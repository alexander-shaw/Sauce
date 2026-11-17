package org.cpts422;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
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

    private Locator productCardByExactName(String productName) {
        // Anchor on the exact user-visible product name, then walk up to the product-card container.
        // (The card wrapper itself has no accessible role/label, so we need DOM traversal here.)
        return page
                .getByText(productName, new Page.GetByTextOptions().setExact(true))
                .locator("xpath=ancestor::div[contains(@class,'inventory_item')]");
    }

    private static BigDecimal parseDisplayedDollars(String displayedAmount) {
        if (displayedAmount == null) {
            throw new IllegalArgumentException("displayedAmount must not be null");
        }

        // Accepts inputs like "$29.99" and "Item total: $29.99".
        // Extract exactly one numeric token; reject malformed inputs (no token or multiple tokens).
        Pattern amountPattern = Pattern.compile("-?\\d+(?:\\.\\d+)?");
        Matcher matcher = amountPattern.matcher(displayedAmount);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No numeric amount found in: " + displayedAmount);
        }
        String numeric = matcher.group();
        if (matcher.find()) {
            throw new IllegalArgumentException("Malformed amount (multiple numeric tokens) in: " + displayedAmount);
        }

        // Convert to BigDecimal and force currency scale (2 decimals) with deterministic rounding.
        return new BigDecimal(numeric).setScale(2, RoundingMode.HALF_UP);
    }

    private void fillCheckoutInformation(String firstName, String lastName, String postalCode) {
        // Use user-facing labels so locators match the real form fields and are reusable in T4.
        page.getByLabel("First Name").fill(firstName);
        page.getByLabel("Last Name").fill(lastName);
        page.getByLabel("Zip/Postal Code").fill(postalCode);
    }

    private void clickContinue() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();
    }

    @Test
    @DisplayName("T1: Successful purchase of two items")
    void t1SuccessfulPurchaseOfTwoItems() {
        loginAs("standard_user", "secret_sauce");
        // Verify the login succeeded by navigating to the products (inventory) page.
        assertThat(page).hasURL("https://www.saucedemo.com/inventory.html");

        // Locate the Backpack product card and read its displayed price as currency.
        Locator backpackCard = productCardByExactName("Sauce Labs Backpack");
        String backpackPriceText = backpackCard
                .locator("xpath=.//*[contains(@class,'inventory_item_price')]")
                .innerText();
        BigDecimal backpackPrice = parseDisplayedDollars(backpackPriceText);

        // Locate the Bike Light product card and read its displayed price as currency.
        Locator bikeLightCard = productCardByExactName("Sauce Labs Bike Light");
        String bikeLightPriceText = bikeLightCard
                .locator("xpath=.//*[contains(@class,'inventory_item_price')]")
                .innerText();
        BigDecimal bikeLightPrice = parseDisplayedDollars(bikeLightPriceText);

        // Add each item to the cart, scoped to its own product card (no navigation yet).
        backpackCard.locator("xpath=.//button[normalize-space()='Add to cart']").click();
        bikeLightCard.locator("xpath=.//button[normalize-space()='Add to cart']").click();

        // CSS fallback: the cart badge has no stable accessible role/label, so locate it directly.
        assertThat(page.locator("span.shopping_cart_badge")).hasText("2");

        // CSS fallback: the cart icon link has no reliable accessible name for getByRole.
        page.locator("a.shopping_cart_link").click();
        assertThat(page).hasURL("https://www.saucedemo.com/cart.html");

        // Verify both items are listed on the cart page (order-independent).
        assertThat(page.getByText("Sauce Labs Backpack")).isVisible();
        assertThat(page.getByText("Sauce Labs Bike Light")).isVisible();

        // Proceed to checkout (step one) and verify we reached the information page.
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Checkout")).click();
        assertThat(page).hasURL("https://www.saucedemo.com/checkout-step-one.html");
        assertThat(page.getByText("Checkout: Your Information")).isVisible();

        // Fill checkout step one with valid data and continue.
        fillCheckoutInformation("John", "Doe", "12345");
        clickContinue();
        assertThat(page).hasURL("https://www.saucedemo.com/checkout-step-two.html");
        assertThat(page.getByText("Checkout: Overview")).isVisible();
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
