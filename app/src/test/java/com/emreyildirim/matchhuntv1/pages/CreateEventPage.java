package com.emreyildirim.matchhuntv1.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class CreateEventPage {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private By locationPermissionBox = AppiumBy.xpath("//android.widget.TextView[@resource-id=\"com.android.permissioncontroller:id/permission_message\"]");
    private By locationPermissionBoxAllow = AppiumBy.xpath("//android.widget.Button[@resource-id=\"com.android.permissioncontroller:id/permission_allow_foreground_only_button\"]");
    private By eventTitleEditText = AppiumBy.xpath("//android.widget.ScrollView/android.widget.EditText[1]");
    private By eventTypeSpinner = AppiumBy.xpath("//android.widget.ScrollView/android.widget.EditText[2]");
    private By eventDescriptionEditText = AppiumBy.xpath("//android.widget.ScrollView/android.widget.EditText[3]");
    private By chooseDateButton = AppiumBy.accessibilityId("Choose Date");
    private By switchTextDateModeButton = AppiumBy.accessibilityId("Switch to text input mode");
    private By chooseTimeButton = AppiumBy.accessibilityId("Choose Time");
    private By chooseLocationButton = AppiumBy.accessibilityId("Choose Location");
    private By approveLocationButton = AppiumBy.accessibilityId("approveLocationButton");
    private By eventParticipantsEditText = AppiumBy.xpath("//android.widget.ScrollView/android.widget.EditText[7]");
    private By createEventButton = AppiumBy.accessibilityId("createEventButton");


    public CreateEventPage(AndroidDriver driver){
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void giveLocationPermission(){
        List<WebElement> elements = driver.findElements(locationPermissionBox);
        if (!elements.isEmpty()){
            wait.until(ExpectedConditions.visibilityOfElementLocated(locationPermissionBox));
            wait.until(ExpectedConditions.elementToBeClickable(locationPermissionBoxAllow)).click();
        }else {
            System.out.println("Location permission box is not displayed");
        }
    }

    private By sportTypeMenuItemByText(String sportType) {
        return AppiumBy.xpath(String.format("//android.widget.TextView[@text=\"%s\"]", sportType));
    }

    public void selectSportTypeFromDropdown(String sportType){
        wait.until(ExpectedConditions.elementToBeClickable(eventTypeSpinner)).click();
        wait.until(ExpectedConditions.elementToBeClickable(sportTypeMenuItemByText(sportType))).click();
    }

    public void pickDateFromInput(String dateValue) {
        // 1) Date picker aç
        wait.until(ExpectedConditions.elementToBeClickable(chooseDateButton)).click();

        wait.until(ExpectedConditions.elementToBeClickable(switchTextDateModeButton)).click();

        // 2) Date input alanını bul (ör: 04/02/2026)
        By dateInput = AppiumBy.xpath("//android.widget.EditText");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dateInput));

        // 3) Temizle + yeni tarihi yaz
        driver.findElement(dateInput).click();
        driver.findElement(dateInput).clear();
        driver.findElement(dateInput).sendKeys(dateValue);


        // 5) Okey ile onayla
        By timeAndDateOkButton = AppiumBy.xpath("//android.widget.TextView[@text=\"Okey\"]");
        wait.until(ExpectedConditions.elementToBeClickable(timeAndDateOkButton)).click();
    }

    public void pickTimeByClock(int hour, int minute) {
        wait.until(ExpectedConditions.elementToBeClickable(chooseTimeButton)).click();

        String hourDesc = hour + " o'clock";
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId(hourDesc)))
                .click();

        String minuteDesc = minute + " minutes";
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId(minuteDesc)))
                .click();

        By okButton = AppiumBy.xpath("//android.widget.TextView[@text=\"Okey\"]");
        wait.until(ExpectedConditions.elementToBeClickable(okButton)).click();
    }

    public void createEvent(String title, String eventType, String description, String dateValue, int participants){
        driver.findElement(eventTitleEditText).sendKeys(title);
        selectSportTypeFromDropdown(eventType);
        wait.until(ExpectedConditions.elementToBeClickable(eventDescriptionEditText)).sendKeys(description);
        pickDateFromInput(dateValue);
        pickTimeByClock(3,15);
        driver.findElement(chooseLocationButton).click();
        wait.until(ExpectedConditions.elementToBeClickable(approveLocationButton)).click();
        driver.findElement(eventParticipantsEditText).sendKeys(String.valueOf(participants));
        wait.until(ExpectedConditions.elementToBeClickable(createEventButton)).click();


    }



}
