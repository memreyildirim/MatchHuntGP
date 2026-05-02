package com.emreyildirim.matchhuntv1.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class FindEventPage {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private final By eventTitleInList = AppiumBy.xpath("//android.widget.TextView[@text=\"TestEvent\"]");

    public FindEventPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void waitForEventTitleInList() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(eventTitleInList));
        Assert.assertTrue(driver.findElement(eventTitleInList).isDisplayed());
    }


}
