package com.emreyildirim.matchhuntv1.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class EventsScreenPage {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private By createNewEventScrollView = AppiumBy.xpath("//android.widget.ScrollView");
    private By tabFindEvent = AppiumBy.accessibilityId("tabFindEvent");
    private By tabCreateEvent = AppiumBy.accessibilityId("tabCreateEvent");

    private By createNewEventText = AppiumBy.xpath("//android.widget.TextView[@text=\"Create New Event\"]");




    public EventsScreenPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void openFindEventTab() {
        wait.until(ExpectedConditions.elementToBeClickable(tabFindEvent)).click();
    }

    public void openCreateEventTab() {
        wait.until(ExpectedConditions.elementToBeClickable(tabCreateEvent)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(createNewEventScrollView));
        Assert.assertEquals("Create New Event", driver.findElement(createNewEventText).getText());
    }

}
