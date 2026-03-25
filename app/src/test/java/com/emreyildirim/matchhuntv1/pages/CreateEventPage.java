package com.emreyildirim.matchhuntv1.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class CreateEventPage {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private By locationPermissionBox = AppiumBy.id("com.android.permissioncontroller:id/grant_dialog");
    private By locaitonPermissionBoxAllow = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_foreground_only_button");


}
