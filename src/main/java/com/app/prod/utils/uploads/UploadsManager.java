package com.app.prod.utils.uploads;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class UploadsManager {

    public static final Path UPLOADS_PATH = Paths.get("src/main/resources/uploads");
    public static final Path ANNOUNCEMENTS_PATH = Paths.get("announcements");
    public static final Path EVENTS_PATH = Paths.get("events");

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final List<Path> allDirectories = List.of(
            resolvePaths(UPLOADS_PATH, ANNOUNCEMENTS_PATH),
            resolvePaths(UPLOADS_PATH, EVENTS_PATH)
    );

    @PostConstruct
    public void initDirectories() {
        for(Path dir : allDirectories) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error("Failed to create directory for path: {}", dir);
                throw new RuntimeException("Failed to create upload directories.", e);
            }
        }
    }

    public Path resolvePaths(Path base, Path sub){
        return base.resolve(sub);
    }

}
