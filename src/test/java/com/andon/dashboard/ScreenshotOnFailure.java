package com.andon.dashboard;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Runs when a test throws, BEFORE @AfterEach quits the browser,
 * so the screenshot captures the failing page.
 */
public class ScreenshotOnFailure implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext ctx, Throwable throwable) throws Throwable {
        Object instance = ctx.getRequiredTestInstance();
        if (instance instanceof AndonDashboardSeleniumTest test) {
            test.takeScreenshotOnFailure(ctx.getRequiredTestMethod().getName());
        }
        throw throwable; // keep the test marked as failed
    }
}