package com.emreyildirim.matchhuntv1.tests;

import com.emreyildirim.matchhuntv1.BaseTest;
import com.emreyildirim.matchhuntv1.pages.LoginPage;

import org.junit.Test;

public class LoginTest extends BaseTest {

    @Test
    public void validLoginTest(){
        LoginPage loginPage = new LoginPage(driver);

        loginPage.givePermission();
        loginPage.validLoginTest("yildirimyedek8@gmail.com","Emre1234");
    }

}




/*
    AndroidDriver driver;

    @Before
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554")
                .setAutomationName("UiAutomator2")
                .setAppPackage("com.emreyildirim.matchhuntv1")//package name
                .setAppActivity("com.emreyildirim.matchhuntv1.MainActivity")
                .setNoReset(false); //false olduğu için her test sonrası tekrar başlatıyor.kaldığı yerden devam etmiyor test case



        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),options);
    }

    @Test
    public void loginTest() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        WebElement permissionBoxAllow = driver.findElement(AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button"));
        permissionBoxAllow.click();

        WebElement emailTextField = driver.findElement(AppiumBy.xpath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View/android.view.View/android.widget.EditText[1]"));
        emailTextField.sendKeys("yildirimyedek8@gmail.com");

        WebElement passwordTextField = driver.findElement(AppiumBy.xpath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View/android.view.View/android.widget.EditText[2]"));
        passwordTextField.sendKeys("Emre1234");

        WebElement loginButton = driver.findElement(AppiumBy.xpath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View/android.view.View/android.view.View[2]/android.widget.Button"));
        loginButton.click();

        WebElement socialFeedText = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//android.widget.TextView[@text=\"Social Feed\"]")));

        Assert.assertEquals("Social Feed", socialFeedText.getText());
    }



    @After
    public void tearDown(){
        if (driver != null){
            //driver.quit();
        }
    }

     */
