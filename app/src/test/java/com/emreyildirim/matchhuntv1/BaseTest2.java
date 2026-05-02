package com.emreyildirim.matchhuntv1;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.emreyildirim.matchhuntv1.utils.ExtentManager;


import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

public class BaseTest2 {

    protected AndroidDriver driver;
    protected ExtentReports extent;
    protected ExtentTest test;

    @BeforeSuite
    public void setUpReport(){
        extent = ExtentManager.getInstance();
    }


    @BeforeMethod
    public void setup(Method method) throws MalformedURLException {
        AppiumTestSupport.skipIfAppiumUnreachable();
        test = extent.createTest(method.getName());
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554")
                .setAutomationName("UiAutomator2")
                .setAppPackage("com.emreyildirim.matchhuntv1")
                .setNoReset(true);

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),options);
        System.out.println("Driver oluşturuldu mu: " + (driver != null));

    }

    @AfterSuite
    public void tearDownReport(){
        //raporu fiziksel dosyaya çevir
        extent.flush();
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
