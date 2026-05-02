package com.emreyildirim.matchhuntv1.tests;

import com.emreyildirim.matchhuntv1.BaseTest;
import com.emreyildirim.matchhuntv1.BaseTest2;
import com.emreyildirim.matchhuntv1.constants.TestSecrets;
import com.emreyildirim.matchhuntv1.pages.CreateEventPage;
import com.emreyildirim.matchhuntv1.pages.EventsScreenPage;
import com.emreyildirim.matchhuntv1.pages.FindEventPage;
import com.emreyildirim.matchhuntv1.pages.LoginPage;
import com.emreyildirim.matchhuntv1.pages.NavigationMenu;

import org.testng.annotations.Test;


public class CreateEventPageTest extends BaseTest2 {

    @Test
    public void createEventTest(){
        NavigationMenu navigationMenu = new NavigationMenu(driver);
        CreateEventPage createEventPage = new CreateEventPage(driver);
        FindEventPage findEventPage = new FindEventPage(driver);
        EventsScreenPage eventsScreenPage = new EventsScreenPage(driver);


        test.info("Uygulamaya giriş yapıldı");
        navigationMenu.goToSearch();
        test.pass("Search sayfasına gidildi");
        eventsScreenPage.openCreateEventTab();
        test.pass("Create Event sayfasına gidildi");
        createEventPage.giveLocationPermission();
        test.pass("Konum izni verildi");
        createEventPage.createEvent("TestEvent", "Football", "Test Description","12242026", 6);
        test.pass("Event oluşturuldu");
        findEventPage.waitForEventTitleInList();
        test.pass("Event listede görünür");


    }


}
