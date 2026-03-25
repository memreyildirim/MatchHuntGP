package com.emreyildirim.matchhuntv1.tests;

import com.emreyildirim.matchhuntv1.BaseTest;
import com.emreyildirim.matchhuntv1.pages.LoginPage;
import com.emreyildirim.matchhuntv1.pages.NavigationMenu;
import com.emreyildirim.matchhuntv1.pages.ProfilePage;

import org.junit.Assert;
import org.junit.Test;

import io.appium.java_client.AppiumBy;

public class LogoutTest extends BaseTest {

    @Test
    public void validLogoutTest(){
        LoginPage loginPage = new LoginPage(driver);
        ProfilePage profilePage = new ProfilePage(driver);
        NavigationMenu navigationMenu = new NavigationMenu(driver);


        loginPage.givePermission();
        loginPage.validLoginTest("yildirimyedek8@gmail.com","Emre1234");
        navigationMenu.goToProfile();
        profilePage.logout();




    }
}
