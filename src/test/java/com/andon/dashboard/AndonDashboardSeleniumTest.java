package com.andon.dashboard;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AndonDashboardSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void teardown(TestInfo testInfo) {
        if (driver != null) {
            driver.quit();
        }
    }

    // Journey 1: Dashboard loads with correct title and summary cards
    @Test
    void testDashboardLoads() {
        driver.get(baseUrl + "/events");
        assertEquals("Shop-Floor Andon Dashboard", driver.getTitle());
        assertTrue(driver.findElement(By.tagName("h1")).getText().contains("Shop-Floor Andon Dashboard"));
    }

    // Journey 2: Log a new event end-to-end
    @Test
    void testLogNewEvent() {
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys("Line 3 - Assembly");
        driver.findElement(By.name("issueType")).sendKeys("Machine Breakdown");
        driver.findElement(By.tagName("button")).click();

        // Should redirect back to dashboard and show the new event
        assertTrue(driver.getCurrentUrl().endsWith("/events"));
        assertTrue(driver.getPageSource().contains("Line 3 - Assembly"));
    }

    // Journey 3: Search functionality filters events
    @Test
    void testSearchEvents() {
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys("Line 5 - Packing");
        driver.findElement(By.name("issueType")).sendKeys("Material Shortage");
        driver.findElement(By.tagName("button")).click();

        driver.get(baseUrl + "/events?q=Packing");
        assertTrue(driver.getPageSource().contains("Line 5 - Packing"));
    }

    // Journey 4: Drill-down into event detail and update status
    @Test
    void testDrillDownAndUpdateStatus() {
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys("Line 2 - Welding");
        driver.findElement(By.name("issueType")).sendKeys("Quality Defect");
        driver.findElement(By.tagName("button")).click();

        WebElement viewLink = driver.findElements(By.linkText("View")).get(0);
        viewLink.click();

        assertTrue(driver.getPageSource().contains("Line 2 - Welding"));

        driver.findElement(By.name("status")).sendKeys("RESOLVED");
        driver.findElement(By.tagName("button")).click();

        assertTrue(driver.getPageSource().contains("RESOLVED"));
    }

    // Journey 5: Critical alert appears for HIGH severity open issue
    @Test
    void testCriticalAlertAppears() {
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys("Line 4 - Paint");
        driver.findElement(By.name("issueType")).sendKeys("Sensor Failure");
        // Select HIGH severity from dropdown
        driver.findElement(By.cssSelector("select[name='severity'] option[value='HIGH']")).click();
        driver.findElement(By.tagName("button")).click();

        driver.get(baseUrl + "/events");
        assertTrue(driver.getPageSource().contains("Critical Alerts"));
        assertTrue(driver.getPageSource().contains("Line 4 - Paint"));
    }

    // Helper: take a screenshot on failure (call manually if a test fails, or wire into a JUnit extension)
    private void takeScreenshotOnFailure(String testName) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destDir = new File("target/selenium-screenshots");
            destDir.mkdirs();
            Files.copy(screenshot.toPath(), new File(destDir, testName + ".png").toPath());
        } catch (Exception e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        }
    }
}