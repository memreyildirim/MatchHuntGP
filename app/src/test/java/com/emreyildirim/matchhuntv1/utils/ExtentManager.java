package com.emreyildirim.matchhuntv1.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.util.Locale;

public class ExtentManager {
    private static ExtentReports extent;

    public static  ExtentReports getInstance(){
        if (extent == null){
            Locale.setDefault(Locale.ENGLISH);
            ExtentSparkReporter spark = new ExtentSparkReporter("build/reports/extent-report.html");
            spark.config().setReportName("Otomasyon Test Sonuçları");
            spark.config().setDocumentTitle("Test Raporu");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("İşletim Sistemi", System.getProperty("os.name"));
            extent.setSystemInfo("Kullanıcı Adı", System.getProperty("user.name"));
        }
        return extent;
    }
}
