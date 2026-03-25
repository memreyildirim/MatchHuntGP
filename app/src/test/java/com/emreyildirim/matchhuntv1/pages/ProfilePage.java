package com.emreyildirim.matchhuntv1.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class ProfilePage {

    private AndroidDriver driver ;
    private WebDriverWait wait;

    private By profileText = AppiumBy.xpath("(//android.widget.TextView[@text=\"Profile\"])[1]");
    private By logoutButton = AppiumBy.xpath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View/android.view.View/android.view.View[1]/android.view.View/android.view.View[2]/android.view.View/android.view.View[3]/android.widget.Button");

    public ProfilePage(AndroidDriver driver){
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void logout(){
        driver.findElement(logoutButton).click();
    }
}
