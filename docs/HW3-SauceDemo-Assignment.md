# HW3: SauceDemo System Testing

## Overview

In this homework you write system tests for **SauceDemo** ([SauceDemo demo shop](https://www.saucedemo.com/)), an online shop built for practicing test automation, using Playwright for Java and JUnit 5. Each test follows one use case thread through the browser, the way a customer would: logging in, choosing products, managing the cart, and checking out. **Money moves in this shop, so your tests must check the amounts, not just the clicks.**

The shop offers several test accounts, all with the password `secret_sauce`, and its login page lists them. This homework uses `standard_user` for normal shopping and `locked_out_user` for a blocked account.

**DO NOT ONE-SHOT THIS HOMEWORK.**

You may use AI throughout, but never generate the test class, or a whole test, from a single prompt. Build the tests thread by thread with AI as a partner. Along with your test file, you submit your **AI logs** and a short **video** explaining one of your tests (see Submission).

## Setup

Use the same setup as the in-class exercise: IntelliJ, Java 17, Maven, Playwright 1.55.0 and JUnit Jupiter 5.10.2. You may reuse the `PlaywrightTest` project from class or create a new Maven project with the same two dependencies. Write all tests in one class, `SauceDemoTest.java`, in `src/test/java/org/cpts422`.

Follow the in-class structure: `Playwright` and `Browser` are created once for the class, and every test gets its own `BrowserContext` and `Page`. Start each test from the login page.

## Use Case Threads to Automate

Write one test per thread below, except where a thread says to use a parameterized test. Start each `@DisplayName` with the thread ID, for example `@DisplayName("T1: Customer buys two items and sees the correct total")`.

Work through the threads one at a time and run each test before you start the next. **Do not one-shot the threads with AI**, not even one thread at a time from a single prompt you did not follow up on.

### T1: Successful purchase of two items

Log in as `standard_user`. On the products page, read the prices shown for **Sauce Labs Backpack** and **Sauce Labs Bike Light**, then add both to the cart. Assert that the cart icon reports 2 items and that the cart page lists both items. Check out with any first name, last name and postal code. On the overview page:

- assert that the item total equals the sum of the two prices your test read from the products page;
- assert that the tax equals 8% of the item total, rounded to the nearest cent;
- assert that the total equals the item total plus the tax.

**Compute the expected amounts in the test from the prices the page shows; do not type the totals in as constants.** Finish the order and assert that the confirmation heading is visible, the cart icon reports an empty cart, and the cart badge is gone.

### T2: Rejected logins

Write **one parameterized test** (`@ParameterizedTest` with `@CsvSource`) covering at least these rows: `locked_out_user` with the correct password, `standard_user` with a wrong password, and an empty username. For each row, assert that the page stays on the login page and that the error message shown matches the case.

**Hint:** in `@CsvSource`, a field left blank arrives in your test as `null`, not as an empty string, and Playwright's `fill(null)` throws an exception. Write the empty username as two single quotes, `''`, which gives an empty string. The same applies to the missing fields in T4.

### T3: Removing an item from the cart

Log in, add two items, open the cart, and remove one. Assert that the cart icon now reports 1 item, the removed item is no longer listed, and the other item still is.

### T4: Checkout information is required

Write **one parameterized test** with a row for each missing field on the checkout information page: first name, last name, and postal code. For each row, fill in the other two fields, press **Continue**, and assert that the page does not move on and that the error names the missing field.

### T5: Sorting products by price

Log in and sort the products by price, low to high, and assert that the sort option is now selected. Read all prices on the page and assert that they appear in non-decreasing order. **Asserting only the first or the last product is not enough.**

### T6: Logging out

Log in, log out through the menu, and assert that the login page is shown. Then navigate directly to the products page URL and assert that the shop refuses access and shows an error on the login page.

## Requirements

- **Locators: the way a user sees the page.** Use `getByRole`, `getByLabel`, `getByPlaceholder` or `getByText` first. Use a CSS locator (`page.locator(...)`) only for an element that has no usable role, label, placeholder or text, such as the cart badge, and add a one-line comment saying why.
- **Playwright assertions for the state of the page.** Use Playwright's `assertThat` (`com.microsoft.playwright.assertions.PlaywrightAssertions`) to check what the page shows: visibility, text, URL and element state. Values your test reads from the page and calculates with, such as prices and totals, may be compared with JUnit assertions.
- **No waiting by hand.** No `Thread.sleep` and no fixed pauses; rely on Playwright's auto-waiting, and raise the timeout of a single assertion if a page is slow.
- **Every test stands alone.** Each test starts from the login page in its own `BrowserContext` and passes when run alone or in any order.
- **Readable tests.** Every test has a `@DisplayName` that starts with its thread ID and a descriptive camelCase method name. Repeated steps, such as logging in, go in a helper method.
- **Tests that can fail.** Every test ends with at least one assertion about the result of the thread, not only the actions that lead to it.
- **Built step by step, not one-shot.** Your AI logs must show the tests being built over several exchanges, thread by thread, with you reading, running and correcting what the AI produced.

## Using AI and Codegen

You may use AI for the whole homework, from understanding Playwright to writing and debugging the tests. **DO NOT ONE-SHOT IT:** never ask an AI to produce the test class, or a whole test, from a single prompt. Work with it thread by thread: ask, read what it gives you, run it against the site, fix what is wrong, and only then move on to the next thread. Keep your **AI logs**, the full conversations with every AI tool you use for this homework; they are part of your submission.

You may also use Playwright's codegen recorder to capture the clicks of a thread. Recorded code is a starting draft, not a finished test: replace any CSS locators that have a role, label, placeholder or text alternative, remove steps the thread does not need, and add the assertions yourself, since a recorder captures what you did, not what should happen.

## Submission

Submit three things:

1. `SauceDemoTest.java`, as a single file (do not zip it and do not submit the whole project).
2. **Your AI logs:** the full conversations with every AI tool you used for this homework, as a PDF, Word or Markdown file.
3. **A video (MP4), 5 minutes at most:** pick one of your tests. With the test open in IntelliJ and SauceDemo open in Chrome, open DevTools (right-click the page and choose **Inspect**, or press Cmd+Option+I on a Mac or Ctrl+Shift+I on Windows) and go to the **Elements** panel. Walk through the test line by line. For each locator, **show the element it finds in the Elements panel** and explain why you chose that locator. For each action, say what it does on the page, and for each assertion, say what it proves about the thread. End by running the test.

**One more time: DO NOT ONE-SHOT THIS HOMEWORK.** The AI logs show how you built your tests, and the video shows that you understand them.

**Before submitting, run the whole class once from IntelliJ and make sure every test passes.**

## Points

| HW3 points breakdown (50 points) | Points |
| --- | ---: |
| T1: Successful purchase of two items | 8 |
| T2: Rejected logins (parameterized) | 5 |
| T3: Removing an item from the cart | 4 |
| T4: Checkout information is required (parameterized) | 5 |
| T5: Sorting products by price | 4 |
| T6: Logging out | 4 |
| Requirements across all tests (locators, assertions, no sleeps, independent tests, naming and helpers) | 5 |
| AI logs | 5 |
| **Video** | **10** |
| Total | **50** |
