package com.emreyildirim.matchhuntv1;

import org.junit.After;
import org.junit.Before;

import java.net.MalformedURLException;
import java.net.URL;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

public class BaseTest {

    protected AndroidDriver driver;

    @Before
    public void setUp() throws MalformedURLException{
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554")
                .setAutomationName("UiAutomator2")
                .setAppPackage("com.emreyildirim.matchhuntv1")//package name
                .setAppActivity("com.emreyildirim.matchhuntv1.MainActivity")
                .setNoReset(false); //false olduğu için her test sonrası tekrar başlatıyor.kaldığı yerden devam etmiyor test case

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),options);
        System.out.println("Driver oluşturuldu mu" + (driver != null));

    }

    @After
    public void tearDown(){
        if (driver != null){
            //driver.quit();
        }
    }
}
