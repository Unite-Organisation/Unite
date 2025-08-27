package com.app.prod.area.api;

import com.app.prod.area.dto.AreaCreateRequest;
import com.app.prod.area.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("arrea")
public class ArreaRestApi {

    private final AreaService areaService;

    @PostMapping()
    public ResponseEntity<String> createArea(@RequestBody AreaCreateRequest request){
        String message = areaService.createArea(request);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{areaId}/add-user/{userId}")
    public ResponseEntity<String> addUser(@PathVariable UUID areaId, @PathVariable UUID userId){
        String message = areaService.addUser(areaId, userId);
        return ResponseEntity.ok(message);
    }

}
