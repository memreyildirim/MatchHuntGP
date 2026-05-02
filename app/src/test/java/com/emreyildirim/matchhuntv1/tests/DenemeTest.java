package com.emreyildirim.matchhuntv1.tests;

import com.emreyildirim.matchhuntv1.BaseTest2;
import com.emreyildirim.matchhuntv1.pages.CreateEventPage;

import org.testng.annotations.Test;


public class DenemeTest extends BaseTest2 {
    @Test
    public void denemeTest(){
        CreateEventPage createEventPage = new CreateEventPage(driver);

    }
}
