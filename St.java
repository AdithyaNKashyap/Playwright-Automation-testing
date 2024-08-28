package Adithya.Playwright;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.WaitForSelectorState;

public class St {

    static class TestResult {
        String step;
        String result;
        String details;
        long duration;

        TestResult(String step, String result, String details, long duration) {
            this.step = step;
            this.result = result;
            this.details = details;
            this.duration = duration;
        }
    }

    static List<TestResult> testResults = new ArrayList<>();

    public static void main(String[] args) {
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        // Start tracing before the tests
        context.tracing().start(new Tracing.StartOptions()
            .setScreenshots(true)
            .setSnapshots(true)
            .setSources(true));

        try {
            performTest(page, "Login Test", () -> loginTest(page));
            
            performTest(page, "Add to Cart Test", () -> addToCartTest(page));
            performTest(page, "Checkout Test", () -> checkoutTest(page));
            System.out.println("All tests completed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
        } finally {
            // Stop tracing after the tests
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("finaltracewithduration.zip")));
            browser.close();
            playwright.close();
        }

        exportTestResultsToExcel("finaltestresultswithduration.xlsx");
    }

    public static void performTest(Page page, String testName, Runnable testMethod) {
        Instant start = Instant.now();
        try {
            testMethod.run();
            testResults.add(new TestResult(testName, "Passed", "", Duration.between(start, Instant.now()).toMillis()));
        } catch (Exception e) {
            testResults.add(new TestResult(testName, "Failed", e.getMessage(), Duration.between(start, Instant.now()).toMillis()));
        }
    }

    public static void loginTest(Page page) {
        page.navigate("https://www.saucedemo.com/");
        page.getByPlaceholder("Username").fill("standard_user");
        page.getByPlaceholder("Password").fill("secret_sauce");
        page.locator("[data-test=login-button]").click();

        // Wait for navigation to complete
        page.waitForURL("https://www.saucedemo.com/inventory.html");
        page.waitForTimeout(2000); // Slow down for visibility

        if (page.url().equals("https://www.saucedemo.com/inventory.html")) {
            System.out.println("Login test passed!");
        } else {
            throw new RuntimeException("Login test failed!");
        }
    }

   

    public static void addToCartTest(Page page) {
        page.navigate("https://www.saucedemo.com/inventory.html");
        page.locator("[data-test=\"add-to-cart-sauce-labs-backpack\"]").click();
        Locator cartBadge = page.locator(".shopping_cart_badge");

        // Ensure the cart badge becomes visible
        cartBadge.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        page.waitForTimeout(2000); // Slow down for visibility

        if (cartBadge.isVisible()) {
            System.out.println("Add to cart test passed!");
        } else {
            throw new RuntimeException("Add to cart test failed!");
        }
    }

    public static void checkoutTest(Page page) {
        page.locator(".shopping_cart_badge").click();
        page.locator("[data-test=\"checkout\"]").click();
        page.getByPlaceholder("First Name").fill("Ashok");
        page.getByPlaceholder("Last Name").fill("Thavamani");
        page.locator("[data-test=\"postalCode\"]").fill("560083");
        page.locator("[data-test=\"continue\"]").click();

        // Wait for navigation to complete
        page.waitForURL("https://www.saucedemo.com/checkout-step-two.html");
        page.waitForTimeout(2000); // Slow down for visibility

        if (page.url().equals("https://www.saucedemo.com/checkout-step-two.html")) {
            page.locator("[data-test='finish']").click();
            System.out.println("Checkout test passed!");
        } else {
            throw new RuntimeException("Checkout test failed at step one!");
        }

        // Wait for navigation to complete
        page.waitForURL("https://www.saucedemo.com/checkout-complete.html");

        if (page.url().equals("https://www.saucedemo.com/checkout-complete.html")) {
            System.out.println("Purchase completed successfully!");
        } else {
            throw new RuntimeException("Checkout test failed at step two!");
        }
    }

    public static void exportTestResultsToExcel(String filePath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Results");

        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Step");
        headerRow.createCell(1).setCellValue("Result");
        headerRow.createCell(2).setCellValue("Details");
        headerRow.createCell(3).setCellValue("Duration (ms)");

        // Data rows
        int rowNum = 1;
        for (TestResult result : testResults) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(result.step);
            row.createCell(1).setCellValue(result.result);
            row.createCell(2).setCellValue(result.details);
            row.createCell(3).setCellValue(result.duration);
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
            System.out.println("Test results exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to export test results: " + e.getMessage());
        }
    }
}