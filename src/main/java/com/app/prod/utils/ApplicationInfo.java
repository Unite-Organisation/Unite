package com.app.prod.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfo {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public AppProfile get() {
        return AppProfile.fromString(activeProfile);
    }

    public boolean isDevEnvironment() {
        return AppProfile.DEV.equals(get());
    }

    public boolean isProdEnvironment() {
        return AppProfile.PROD.equals(get());
    }

}
