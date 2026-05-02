package com.emreyildirim.matchhuntv1;

import com.emreyildirim.matchhuntv1.constants.TestSecrets;
import com.emreyildirim.matchhuntv1.pages.LoginPage;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.net.MalformedURLException;
import java.net.URL;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

public class BaseTest {

    protected AndroidDriver driver;

    @BeforeTest
    public void setUp() throws MalformedURLException{
        AppiumTestSupport.skipIfAppiumUnreachable();
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554")
                .setAutomationName("UiAutomator2")
                .setAppPackage("com.emreyildirim.matchhuntv1")//package name
                .setAppActivity("com.emreyildirim.matchhuntv1.MainActivity")
                .setNoReset(false); //false olduğu için her test sonrası tekrar başlatıyor.kaldığı yerden devam etmiyor test case

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),options);
        System.out.println("Driver oluşturuldu mu" + (driver != null));

        ensuredLoggedIn();


    }

    @AfterTest
    public void tearDown(){
        if (driver != null){
            driver.quit();
        }
    }

    public void ensuredLoggedIn(){
        LoginPage loginPage = new LoginPage(driver);
        loginPage.givePermission();
        loginPage.validLogin(TestSecrets.getUsername(), TestSecrets.getPassword());
    }
}
