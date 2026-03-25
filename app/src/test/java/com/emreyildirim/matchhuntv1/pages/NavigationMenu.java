package com.emreyildirim.matchhuntv1.pages;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;


import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class NavigationMenu {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private By socialBottomMenuButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.view.View\").instance(33)");
    private By searchBottomMenuButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.view.View\").instance(35)");
    private By eventsBottomManuButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.view.View\").instance(37)");
    private By profileBottomMenuButton = AppiumBy.xpath("//android.view.View[@content-desc=\"Profile\"]");

    private By profileText = AppiumBy.xpath("(//android.widget.TextView[@text=\"Profile\"])[1]");


    public NavigationMenu(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void goToSocialFeed() {
        driver.findElement(socialBottomMenuButton).click();
    }

    public void goToSearch() {
        driver.findElement(searchBottomMenuButton).click();
    }

    public void goToEvents() {
        driver.findElement(eventsBottomManuButton).click();
    }

    public void goToProfile() {
        driver.findElement(profileBottomMenuButton).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(profileText));
        Assert.assertEquals("Profile", driver.findElement(profileText).getText());
    }
}
