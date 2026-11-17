package org.cpts422;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    private static BigDecimal parseLabeledDollars(String displayedText, String label) {
        if (displayedText == null) {
            throw new IllegalArgumentException("displayedText must not be null");
        }
        if (label == null) {
            throw new IllegalArgumentException("label must not be null");
        }

        // Extract the single numeric token that follows the given label (e.g., "Tax: $3.20").
        Pattern amountPattern = Pattern.compile(
                Pattern.quote(label) + "\\s*\\$?(-?\\d+(?:\\.\\d+)?)"
        );
        Matcher matcher = amountPattern.matcher(displayedText);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No numeric amount found for label '" + label + "' in: " + displayedText);
        }
        String numeric = matcher.group(1);
        if (matcher.find()) {
            throw new IllegalArgumentException("Malformed labeled amount (multiple matches) for label '" + label + "' in: " + displayedText);
        }

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

    private void clickAddToCart(Locator productCard) {
        productCard.locator("xpath=.//button[normalize-space()='Add to cart']").click();
    }

    private void openCart() {
        // CSS fallback: the cart icon link has no reliable accessible name for user-facing role-based locators.
        page.locator("a.shopping_cart_link").click();
    }

    @Test
    @DisplayName("T1: Successful purchase of two items")
    void t1SuccessfulPurchaseOfTwoItems() {
        loginAs("standard_user", "secret_sauce");
        // Verify the login succeeded by navigating to the products (inventory) page.
        assertThat(page).hasURL("https://www.saucedemo.com/inventory.html");

        // Locate the Backpack product card and read its displayed price as currency.
        Locator backpackCard = productCardByExactName("Sauce Labs Backpack");
        // XPath fallback: the price text varies and has no role, label, or placeholder to target.
        String backpackPriceText = backpackCard
                .locator("xpath=.//*[contains(@class,'inventory_item_price')]")
                .innerText();
        BigDecimal backpackPrice = parseDisplayedDollars(backpackPriceText);

        // Locate the Bike Light product card and read its displayed price as currency.
        Locator bikeLightCard = productCardByExactName("Sauce Labs Bike Light");
        // XPath fallback: the price text varies and has no role, label, or placeholder to target.
        String bikeLightPriceText = bikeLightCard
                .locator("xpath=.//*[contains(@class,'inventory_item_price')]")
                .innerText();
        BigDecimal bikeLightPrice = parseDisplayedDollars(bikeLightPriceText);

        // Add each item to the cart, scoped to its own product card (no navigation yet).
        clickAddToCart(backpackCard);
        clickAddToCart(bikeLightCard);

        assertThat(page.getByText("2", new Page.GetByTextOptions().setExact(true))).hasCount(1);

        openCart();
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

        // Calculate expected item subtotal from the product prices shown on the inventory page.
        BigDecimal expectedItemSubtotal = backpackPrice.add(bikeLightPrice).setScale(2, RoundingMode.HALF_UP);

        // Read the checkout overview totals from a single text block and parse labeled amounts from it.
        Locator itemTotalLabel = page.getByText("Item total:");
        String checkoutOverviewText = itemTotalLabel.locator("xpath=ancestor::*[contains(., '$')][1]").innerText();

        BigDecimal displayedItemSubtotal = parseLabeledDollars(checkoutOverviewText, "Item total:");
        assertEquals(expectedItemSubtotal, displayedItemSubtotal);

        // Calculate expected tax as 8% of the item subtotal, rounded to currency scale.
        BigDecimal expectedTax = expectedItemSubtotal
                .multiply(new BigDecimal("0.08"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal displayedTax = parseLabeledDollars(checkoutOverviewText, "Tax:");
        assertEquals(expectedTax, displayedTax);

        // Calculate expected final total from the expected subtotal and rounded tax.
        BigDecimal expectedFinalTotal = expectedItemSubtotal
                .add(expectedTax)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal displayedFinalTotal = parseLabeledDollars(checkoutOverviewText, "Total:");
        assertEquals(expectedFinalTotal, displayedFinalTotal);

        // Finish the purchase and verify confirmation + empty-cart UI.
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Finish")).click();

        // Verify the order confirmation heading is visible.
        assertThat(page.getByText("THANK YOU FOR YOUR ORDER")).isVisible();

        // Verify the cart icon remains available but the cart badge is absent (empty cart).
        // CSS fallback: the cart icon link has no reliable accessible name for user-facing role-based locators.
        assertThat(page.locator("a.shopping_cart_link")).isVisible();
        // CSS fallback: the cart badge is not reliably targetable via user-facing locators, so locate it directly.
        assertThat(page.locator("span.shopping_cart_badge")).hasCount(0);

        // Strengthen empty-cart verification: the cart page should not contain the previously purchased items.
        openCart();
        assertThat(page).hasURL("https://www.saucedemo.com/cart.html");
        assertThat(page.getByText("Sauce Labs Backpack")).hasCount(0);
        assertThat(page.getByText("Sauce Labs Bike Light")).hasCount(0);
    }

    @ParameterizedTest
    @CsvSource({
        "locked_out_user, secret_sauce, 'Epic sadface: Sorry, this user has been locked out.'",
        "standard_user, wrong_password, Epic sadface: Username and password do not match any user in this service",
        "'', secret_sauce, Epic sadface: Username is required"
    })
    @DisplayName("T2: Rejected logins show expected error messages")
    void t2RejectedLoginsShowExpectedErrorMessages(String username, String password, String expectedErrorMessage) {
        // loginAs only fills + clicks the Login button; it does not assert success.
        loginAs(username, password);

        // The error message alone isn't sufficient; assert the app didn't navigate away from the login page.
        assertThat(page).hasURL("https://www.saucedemo.com/");

        // The login error is shown as user-visible text, so use an exact getByText locator.
        Locator expectedErrorLocator = page.getByText(
                expectedErrorMessage,
                new Page.GetByTextOptions().setExact(true)
        );
        assertThat(expectedErrorLocator).isVisible();
        assertThat(expectedErrorLocator).hasText(expectedErrorMessage);
    }

    @Test
    @DisplayName("T3: Removing an item from the cart")
    void t3RemovingOneItemFromCart() {
        loginAs("standard_user", "secret_sauce");

        // Add each item to the cart, scoped to its own product card.
        Locator backpackCard = productCardByExactName("Sauce Labs Backpack");
        clickAddToCart(backpackCard);

        Locator bikeLightCard = productCardByExactName("Sauce Labs Bike Light");
        clickAddToCart(bikeLightCard);

        assertThat(page.getByText("2", new Page.GetByTextOptions().setExact(true))).hasCount(1);

        openCart();
        assertThat(page).hasURL("https://www.saucedemo.com/cart.html");

        // Verify both selected items are listed on the cart page (order-independent).
        assertThat(page.getByText("Sauce Labs Backpack")).isVisible();
        assertThat(page.getByText("Sauce Labs Bike Light")).isVisible();

        // Remove only Sauce Labs Backpack (scoped to its specific cart item).
        Locator backpackCartItem = page.getByText(
                        "Sauce Labs Backpack",
                        new Page.GetByTextOptions().setExact(true)
                )
                .locator("xpath=ancestor::*[.//button[normalize-space()='Remove'] and count(.//button[normalize-space()='Remove'])=1]");
        backpackCartItem.getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Remove")
                )
                .click();

        // CSS fallback: the cart badge has no stable accessible role/label, so locate it directly.
        assertThat(page.locator("span.shopping_cart_badge")).hasText("1");

        // Assert the removed item is truly absent (not just hidden).
        assertThat(page.getByText("Sauce Labs Backpack")).hasCount(0);
        assertThat(page.getByText("Sauce Labs Bike Light")).isVisible();
    }

    @ParameterizedTest
    @CsvSource({
        "'', Doe, 12345, 'Error: First Name is required'",
        "John, '', 12345, 'Error: Last Name is required'",
        "John, Doe, '', 'Error: Postal Code is required'"
    })
    @DisplayName("T4: Checkout information is required")
    void t4CheckoutInformationIsRequiredShowsMissingFieldErrors(
            String firstName,
            String lastName,
            String postalCode,
            String expectedErrorMessage
    ) {
        loginAs("standard_user", "secret_sauce");

        // Add one item to the cart (use the existing product-card locator strategy).
        Locator backpackCard = productCardByExactName("Sauce Labs Backpack");
        clickAddToCart(backpackCard);

        openCart();
        assertThat(page).hasURL("https://www.saucedemo.com/cart.html");
        assertThat(page.getByText("Sauce Labs Backpack")).isVisible();

        // Proceed to checkout (step one) and verify we reached the information page.
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Checkout")).click();
        assertThat(page).hasURL("https://www.saucedemo.com/checkout-step-one.html");
        assertThat(page.getByText("Checkout: Your Information")).isVisible();

        // Fill checkout information using the parameterized values (including empty strings for required-field negatives).
        fillCheckoutInformation(firstName, lastName, postalCode);

        // Exercise checkout-step-one validation for whichever single field is empty in this CSV row.
        clickContinue();

        // The missing-field submission should not advance to step two.
        assertThat(page).hasURL("https://www.saucedemo.com/checkout-step-one.html");

        // The validation error is shown as user-visible text, so use an exact getByText locator.
        Locator expectedErrorLocator = page.getByText(
                expectedErrorMessage,
                new Page.GetByTextOptions().setExact(true)
        );
        assertThat(expectedErrorLocator).isVisible();
        assertThat(expectedErrorLocator).hasText(expectedErrorMessage);
    }

    @Test
    @DisplayName("T5: Sorting products by ascending price")
    void t5SortProductsByPriceLowToHigh() {
        loginAs("standard_user", "secret_sauce");
        assertThat(page).hasURL("https://www.saucedemo.com/inventory.html");

        // Select the user-visible "Price (low to high)" option from the sort dropdown.
        Locator sortDropdown = page.getByRole(AriaRole.COMBOBOX);
        sortDropdown.selectOption(new SelectOption().setLabel("Price (low to high)"));

        // Assert the ascending sort option is selected by its underlying <option value>.
        assertThat(sortDropdown).hasValue("lohi");

        // Read every displayed product price in DOM order and convert to currency BigDecimal values.
        Locator priceLabels = page.getByText(Pattern.compile("^\\$\\d+\\.\\d{2}$"));
        var displayedPrices = priceLabels.allInnerTexts()
                .stream()
                .map(SauceDemoTest::parseDisplayedDollars)
                .toList();

        // Prevent vacuous passes: sorting correctness needs at least two values to compare.
        org.junit.jupiter.api.Assertions.assertTrue(
                displayedPrices.size() > 1,
                "Expected multiple displayed product prices to validate ascending sorting"
        );

        // Prevent partial-collection passes: ensure we extracted one price per product card.
        int addToCartButtonCount = page
                .getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to cart"))
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(
                addToCartButtonCount,
                displayedPrices.size(),
                "Expected to collect a displayed price for every product shown on the page"
        );

        // Verify non-decreasing order across every adjacent pair (allowing equal prices).
        for (int i = 0; i < displayedPrices.size() - 1; i++) {
            BigDecimal earlier = displayedPrices.get(i);
            BigDecimal later = displayedPrices.get(i + 1);
            org.junit.jupiter.api.Assertions.assertTrue(
                    earlier.compareTo(later) <= 0,
                    "Ascending price sort failed at adjacent indices " + i + " and " + (i + 1)
                            + ": expected " + earlier + " <= " + later
            );
        }
    }

    @Test
    @DisplayName("T6: Logging out")
    void t6LogoutAndProtectProductsPage() {
        loginAs("standard_user", "secret_sauce");
        assertThat(page).hasURL("https://www.saucedemo.com/inventory.html");

        // Open the sidebar menu (user-facing button name).
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open Menu")).click();

        // Auto-wait verification that the logout control is now visible.
        assertThat(page.getByText("Logout", new Page.GetByTextOptions().setExact(true))).isVisible();

        // Click logout to end the authenticated session.
        page.getByText("Logout", new Page.GetByTextOptions().setExact(true)).click();

        // Verify logout returned us to the login page.
        assertThat(page).hasURL("https://www.saucedemo.com/");
        assertThat(page.getByLabel("Username")).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"))).isVisible();

        // Navigate directly to the protected products page in the *same* session and verify it is blocked.
        page.navigate("https://www.saucedemo.com/inventory.html");
        assertThat(page).hasURL("https://www.saucedemo.com/");

        String expectedUnauthorizedAccessError =
                "Epic sadface: You can only access '/inventory.html' when you are logged in.";
        Locator unauthorizedAccessError = page.getByText(
                expectedUnauthorizedAccessError,
                new Page.GetByTextOptions().setExact(true)
        );
        assertThat(unauthorizedAccessError).isVisible();
        assertThat(unauthorizedAccessError).hasText(expectedUnauthorizedAccessError);
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
