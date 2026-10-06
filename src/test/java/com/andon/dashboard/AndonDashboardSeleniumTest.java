package com.andon.dashboard;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Week 9 - Selenium WebDriver suite for the Shop-Floor Andon Dashboard.
 * Five critical user journeys (TC-01 .. TC-05). Failure screenshots are saved to
 * target/selenium-screenshots/ by the ScreenshotOnFailure extension.
 */
@ExtendWith(ScreenshotOnFailure.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AndonDashboardSeleniumTest {

    private static final String EVENTS_URL_REGEX = ".*/events(\\?.*)?/?$";

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
        options.addArguments("--headless=new", "--window-size=1366,900");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /** Unique station name so tests never collide on shared in-memory data. */
    private String uniqueStation(String base) {
        return base + " #" + (System.currentTimeMillis() % 100000);
    }

    /** Waits until the post-submit redirect to the events list has completed. */
    private void waitForEventsList() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.urlMatches(EVENTS_URL_REGEX));
    }

    /** Fills and submits the new-event form, then waits for the redirect. */
    private void logEvent(String station, String issueType) {
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys(station);
        driver.findElement(By.name("issueType")).sendKeys(issueType);
        driver.findElement(By.tagName("button")).click();
        waitForEventsList();
    }

    // TC-01: Dashboard loads with correct title and heading
    @Test
    void testDashboardLoads() {
        driver.get(baseUrl + "/events");
        assertEquals("Shop-Floor Andon Dashboard", driver.getTitle());
        assertTrue(driver.findElement(By.tagName("h1")).getText().contains("Shop-Floor Andon Dashboard"));
    }

    // TC-02: Log a new event end-to-end
    @Test
    void testLogNewEvent() {
        String station = uniqueStation("Line 3 - Assembly");
        logEvent(station, "Machine Breakdown");

        assertTrue(driver.getCurrentUrl().matches(EVENTS_URL_REGEX),
                   "URL was: " + driver.getCurrentUrl());
        assertTrue(driver.getPageSource().contains(station));
    }

    // TC-03: Search filters events
    @Test
    void testSearchEvents() {
        String station = uniqueStation("Line 5 - Packing");
        logEvent(station, "Material Shortage");

        driver.get(baseUrl + "/events?q=Packing");
        assertTrue(driver.getPageSource().contains(station));
    }

    // TC-04: Drill-down into event detail and update status
    @Test
    void testDrillDownAndUpdateStatus() {
        String station = uniqueStation("Line 2 - Welding");
        logEvent(station, "Quality Defect");

        driver.findElement(By.xpath("//tr[contains(.,'" + station + "')]"))
              .findElement(By.linkText("View")).click();
        assertTrue(driver.getPageSource().contains(station));

        // The app reloads the detail page (/events/{id}) after saving, so wait for
        // the old form to go stale instead of waiting for the /events list URL.
        WebElement statusField = driver.findElement(By.name("status"));
        new Select(statusField).selectByValue("RESOLVED");
        driver.findElement(By.tagName("button")).click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.stalenessOf(statusField));

        driver.get(baseUrl + "/events");
        String rowText = driver.findElement(By.xpath("//tr[contains(.,'" + station + "')]")).getText();
        assertTrue(rowText.toUpperCase().contains("RESOLVED"), "Row was: " + rowText);
    }

    // TC-05: Critical alert appears for HIGH severity open issue
    @Test
    void testCriticalAlertAppears() {
        String station = uniqueStation("Line 4 - Paint");
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys(station);
        driver.findElement(By.name("issueType")).sendKeys("Sensor Failure");
        new Select(driver.findElement(By.name("severity"))).selectByValue("HIGH");
        driver.findElement(By.tagName("button")).click();
        waitForEventsList();

        driver.get(baseUrl + "/events");
        assertTrue(driver.getPageSource().contains("Critical Alerts"));
        assertTrue(driver.getPageSource().contains(station));
    }

    /** Called automatically by ScreenshotOnFailure when a test fails. */
    void takeScreenshotOnFailure(String testName) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destDir = new File("target/selenium-screenshots");
            destDir.mkdirs();
            Files.copy(screenshot.toPath(),
                       new File(destDir, testName + ".png").toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        }
    }
}