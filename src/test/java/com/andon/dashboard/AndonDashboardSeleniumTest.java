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
        loginAs("admin", "admin123");
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void loginAs(String user, String pass) {
        driver.get(baseUrl + "/login");
        driver.findElement(By.name("username")).sendKeys(user);
        driver.findElement(By.name("password")).sendKeys(pass);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        waitForEventsList();
    }

    private String uniqueStation(String base) {
        return base + " #" + (System.currentTimeMillis() % 100000);
    }

    private void waitForEventsList() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.urlMatches(EVENTS_URL_REGEX));
    }

    private void logEvent(String station, String issueType) {
        driver.get(baseUrl + "/events/new");
        driver.findElement(By.name("station")).sendKeys(station);
        driver.findElement(By.name("issueType")).sendKeys(issueType);
        driver.findElement(By.tagName("button")).click();
        waitForEventsList();
    }

    // TC-01
    @Test
    void testDashboardLoads() {
        driver.get(baseUrl + "/events");
        assertEquals("Shop-Floor Andon Dashboard", driver.getTitle());
        assertTrue(driver.findElement(By.tagName("h1")).getText().contains("Shop-Floor Andon Dashboard"));
    }

    // TC-02
    @Test
    void testLogNewEvent() {
        String station = uniqueStation("Line 3 - Assembly");
        logEvent(station, "Machine Breakdown");
        assertTrue(driver.getCurrentUrl().matches(EVENTS_URL_REGEX), "URL was: " + driver.getCurrentUrl());
        assertTrue(driver.getPageSource().contains(station));
    }

    // TC-03
    @Test
    void testSearchEvents() {
        String station = uniqueStation("Line 5 - Packing");
        logEvent(station, "Material Shortage");
        driver.get(baseUrl + "/events?q=Packing");
        assertTrue(driver.getPageSource().contains(station));
    }

    // TC-04
    @Test
    void testDrillDownAndUpdateStatus() {
        String station = uniqueStation("Line 2 - Welding");
        logEvent(station, "Quality Defect");

        driver.findElement(By.xpath("//tr[contains(.,'" + station + "')]"))
              .findElement(By.linkText("View")).click();
        assertTrue(driver.getPageSource().contains(station));

        WebElement statusField = driver.findElement(By.name("status"));
        new Select(statusField).selectByValue("RESOLVED");
        driver.findElement(By.tagName("button")).click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.stalenessOf(statusField));

        driver.get(baseUrl + "/events");
        String rowText = driver.findElement(By.xpath("//tr[contains(.,'" + station + "')]")).getText();
        assertTrue(rowText.toUpperCase().contains("RESOLVED"), "Row was: " + rowText);
    }

    // TC-05
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

    // TC-06: anonymous users are sent to login
    @Test
    void testAnonymousRedirectedToLogin() {
        driver.manage().deleteAllCookies();
        driver.get(baseUrl + "/events");
        assertTrue(driver.getCurrentUrl().contains("/login"), "URL was: " + driver.getCurrentUrl());
    }

    // TC-07: viewer is read-only
    @Test
    void testViewerIsReadOnly() {
        driver.manage().deleteAllCookies();
        loginAs("viewer", "viewer123");

        assertFalse(driver.getPageSource().contains("Log New Event"),
                    "Viewer should not see the Log New Event button");

        driver.get(baseUrl + "/events/new");
        assertTrue(driver.getPageSource().contains("403"),
                   "Expected a 403 page for viewer on /events/new");
    }

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