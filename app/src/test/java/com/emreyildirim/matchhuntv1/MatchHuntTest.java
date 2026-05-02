package com.emreyildirim.matchhuntv1;

import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

public class MatchHuntTest extends  BaseTest {

    @Test
    public void testCreateButtonClick(){
        //1.Elementi bekle ve bul(xpath ile)
        WebElement createBtn = driver.findElement(AppiumBy.xpath("//android.view.View[@content-desc=\"Create\"]"));

        //2.aksiyon al : tıkla
        createBtn.click();

        System.out.println("Tıklama işlemi başarılı");
    }

}
