package com.emreyildirim.matchhuntv1.tests;

import com.emreyildirim.matchhuntv1.BaseTest;
import com.emreyildirim.matchhuntv1.constants.TestSecrets;
import com.emreyildirim.matchhuntv1.pages.LoginPage;
import com.emreyildirim.matchhuntv1.pages.NavigationMenu;
import com.emreyildirim.matchhuntv1.pages.ProfilePage;

import org.testng.annotations.Test;


public class ProfilePageTest extends BaseTest {

    @Test
    public void validLogoutTest(){
        LoginPage loginPage = new LoginPage(driver);
        ProfilePage profilePage = new ProfilePage(driver);
        NavigationMenu navigationMenu = new NavigationMenu(driver);


        loginPage.givePermission();
        loginPage.validLogin(TestSecrets.getUsername(), TestSecrets.getPassword());
        navigationMenu.goToProfile();
        profilePage.logout();




    }
}
