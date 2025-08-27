package com.app.prod.area.dto;

import com.app.prod.area.enums.AreaType;

public record AreaCreateRequest(
        String name,
        String country,
        String city,
        AreaType type
) {}