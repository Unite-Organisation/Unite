package com.app.prod.healthcheck;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalTime;

@RestController
@RequestMapping("healthcheck")
@RequiredArgsConstructor
@Tag(name = "Healthcheck")
public class HealthcheckController {

    private final Clock clock;

    @GetMapping()
    public ResponseEntity<String> healthCheck(){
        return ResponseEntity.ok(String.format("[%s] App stable and running.", LocalTime.now(clock)));
    }
}
