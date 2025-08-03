package com.app.prod.healthcheck;

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
public class HealthcheckController {

    private final Clock clock;

    @GetMapping()
    public ResponseEntity<String> healthCheck(){
        return ResponseEntity.ok(String.format("[%s] App stable and running.", LocalTime.now(clock)));
    }
}
