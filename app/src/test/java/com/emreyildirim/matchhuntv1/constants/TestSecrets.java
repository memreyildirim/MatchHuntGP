package com.emreyildirim.matchhuntv1.constants;


import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public final class TestSecrets {
    private TestSecrets() {}

    private static String read(String key) {
        try {
            File localProps = new File(System.getProperty("user.dir"), "local.test.secrets.properties");
            if (!localProps.exists()) return null;

            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(localProps)) {
                p.load(fis);
            }
            return p.getProperty(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getUsername() {
        return read("TEST_USERNAME");
    }

    public static String getPassword() {
        return read("TEST_PASSWORD");
    }

    public static void requireCredsOrSkip() {
        String username = getUsername();
        String password = getPassword();

//        Assume.assumeTrue(username != null && !username.isBlank());
//        Assume.assumeTrue(password != null && !password.isBlank());
    }
}
