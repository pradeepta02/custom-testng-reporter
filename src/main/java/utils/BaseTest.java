package utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.TestListenerAdapter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;

public class BaseTest extends TestListenerAdapter
{
    protected WebDriver driver;

    @Parameters({ "os", "os_version", "browser", "browser_version" })
    @BeforeTest(alwaysRun = true)
    public void setUp(String os, String os_version, String browser, String browser_version)
    {
        System.setProperty("os", os);
        System.setProperty("os_version", os_version);
        System.setProperty("browser", browser);
        System.setProperty("browser_version", browser_version);

        driver = new FirefoxDriver();
        driver.get(PropertyUtils.getProperty("app.url"));

        new WebDriverWait(driver, 25).until(ExpectedConditions.titleContains("Google"));
    }

    @AfterTest(alwaysRun = true)
    public void tearDown()
    {
        driver.close();
        driver.quit();
    }

    public WebDriver getDriverInstance()
    {
        return driver;
    }

    @Override
    public void onTestSkipped(ITestResult result)
    {
        onTestFailure(result);
    }

    @Override
    public void onTestFailure(ITestResult result)
    {
        Object currentClass = result.getInstance();
        WebDriver driver = ((BaseTest) currentClass).getDriverInstance();

        if (driver != null)
        {
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            try
            {
                String fileNameToCopy = "target/custom-test-reports/" + result.getTestClass().getName()
                        + "_screenshot.png";
                FileUtils.copyFile(scrFile, new File(fileNameToCopy));
                Reporter.log("[Console Log] Screenshot saved in " + result.getTestClass().getName() + "_screenshot.png");
            } catch (IOException ex)
            {
                // Log error message
            }
        }
    }
}
