package com.app.prod.area.api;

import com.app.prod.area.dto.AreaCreateRequest;
import com.app.prod.area.service.AreaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("area")
@Tag(name = "Areas")
public class ArreaRestApi {

    private final AreaService areaService;

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> createArea(@RequestBody AreaCreateRequest request){
        String message = areaService.createArea(request);
        return ResponseEntity.ok(message);
    }

}
