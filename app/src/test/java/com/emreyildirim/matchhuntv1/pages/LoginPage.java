package com.emreyildirim.matchhuntv1.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class LoginPage {
    private AndroidDriver driver ;
    private WebDriverWait wait;

    //locators(Adresler)
    private By emailTextField = AppiumBy.xpath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View/android.view.View/android.widget.EditText[1]"); //testTag ile en önerilen
    private By passwordTextField = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(1)"); //uiautomator daha zorululuk
    //private By passwordTextField = AppiumBy.androidUIAutomator("new UiSelector().textContains(\\\"Şifre\\\")");
    private By loginButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(2)");
    private By forgotPasswordButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(1)");
    private By registerButton = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.Button\").instance(3)");
    private By permissionMessage = AppiumBy.id("com.android.permissioncontroller:id/permission_message");
    private By permissionBoxAllow = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button");
    private By permissionBoxDeny = AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button");
    private By socialFeedText = AppiumBy.xpath("//android.widget.TextView[@text=\"Social Feed\"]");

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void validLogin(String email, String password){
        driver.findElement(emailTextField).sendKeys(email);
        driver.findElement(passwordTextField).sendKeys(password);
        driver.findElement(loginButton).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(socialFeedText));
        Assert.assertEquals("Social Feed", driver.findElement(socialFeedText).getText());
    }

    public void givePermission(){
        wait.until(ExpectedConditions.visibilityOfElementLocated(permissionMessage));
        driver.findElement(permissionBoxAllow).click();
    }


//    public void loginAndLogout(String email, String password){
//        driver.findElement(permissionBoxAllow).click();
//        wait.until(ExpectedConditions.visibilityOfElementLocated(emailTextField)).sendKeys(email);
//        driver.findElement(passwordTextField).sendKeys(password);
//        driver.findElement(loginButton).click();
//        wait.until(ExpectedConditions.visibilityOfElementLocated(socialFeedText));
//        driver.findElement(profileBottomMenuButton).click();
//        wait.until(ExpectedConditions.visibilityOfElementLocated(profileText));
//        driver.findElement(logoutButton).click();
//
//
//    }
}
