package com.emreyildirim.matchhuntv1.tests;

import com.emreyildirim.matchhuntv1.BaseTest;
import com.emreyildirim.matchhuntv1.constants.TestSecrets;
import com.emreyildirim.matchhuntv1.pages.LoginPage;

import org.testng.annotations.Test;


public class LoginPageTest extends BaseTest {

    @Test
    public void validLoginTest(){
        LoginPage loginPage = new LoginPage(driver);

        loginPage.givePermission();
        loginPage.validLogin(TestSecrets.getUsername(),TestSecrets.getPassword());
    }
}
