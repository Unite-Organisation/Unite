package com.app.prod.utils;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationInfo {

    private final Environment environment;

    public AppProfile get() {
        String[] active = environment.getActiveProfiles();
        String[] profiles = active.length > 0 ? active : environment.getDefaultProfiles();

        return Arrays.stream(profiles)
                .map(AppProfile::match)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalApplicationStateException(AppError.of(Code.APP_PROFILE_NOT_FOUND)));
    }

    public boolean isDevEnvironment() {
        return AppProfile.DEV == get();
    }

    public boolean isProdEnvironment() {
        return AppProfile.PROD == get();
    }
}
