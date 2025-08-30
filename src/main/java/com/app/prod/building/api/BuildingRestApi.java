package com.app.prod.building.api;

import com.app.prod.building.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("buildings")
@RequiredArgsConstructor
public class BuildingRestApi {

    private final BuildingService buildingService;

}
