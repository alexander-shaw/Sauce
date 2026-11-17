# HW3: SauceDemo System Testing

CPTS 422 homework: browser system tests for [SauceDemo](https://www.saucedemo.com/) using Playwright for Java 1.55.0 and JUnit Jupiter 5.10.2 on Java 17 (Maven). The full assignment is in [docs/HW3-SauceDemo-Assignment.md](docs/HW3-SauceDemo-Assignment.md).

## What was built

All tests are in [src/test/java/org/cpts422/SauceDemoTest.java](src/test/java/org/cpts422/SauceDemoTest.java). One `Playwright` and `Browser` are shared by the class. Each test, and each parameterized row, gets its own `BrowserContext` and `Page` and starts on the login page.

| Thread | Test | What it checks |
|---|---|---|
| T1 | Successful purchase of two items | Prices read from the page; item total, 8% tax and total computed with `BigDecimal`; confirmation shown and cart emptied |
| T2 | Rejected logins (3 CSV rows) | Locked-out user, wrong password and empty username stay on the login page and show the exact error |
| T3 | Removing an item from the cart | Badge drops to 1, the removed item is gone and the other remains |
| T4 | Checkout information is required (3 CSV rows) | Each missing field keeps checkout on step one and the error names that field |
| T5 | Sorting products by price | "Price (low to high)" is selected and every adjacent pair of prices is in non-decreasing order |
| T6 | Logging out | Logout returns to the login page; opening `/inventory.html` directly is refused with an error |

The tests use user-facing locators (`getByRole`, `getByLabel`, `getByText`). CSS or XPath appears only where an element has no usable role, label or text, with a one-line comment. There are no fixed waits.

## Running

```sh
mvn test
```

Or run `SauceDemoTest` from IntelliJ with JDK 17. The latest run had 10 test runs, all passing (4 tests plus 3 rows each for T2 and T4).

## AI logs

The AI conversations used to build the tests are in [docs/logs.zip](docs/logs.zip).
