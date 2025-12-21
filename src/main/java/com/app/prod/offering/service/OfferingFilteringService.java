package com.app.prod.offering.service;

import com.app.prod.area.service.AreaService;
import com.app.prod.building.service.BuildingService;
import com.app.prod.offering.enums.OfferingCategory;
import com.app.prod.utils.filters.OfferingFilter;
import com.app.prod.utils.filters.PriceFilter;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OfferingFilteringService {

    private final AreaService areaService;

    public OfferingFilter prepareFilter(
            AppUserRecord user,
            OfferingCategory category,
            BigDecimal price,
            PriceFilter.PriceModifier modifier
    ) {

        var areaId = areaService.getUserArea(user);
        PriceFilter priceFilter = PriceFilter.builder()
                .price(Optional.ofNullable(price))
                .priceModifier(modifier)
                .build();

        return OfferingFilter.builder()
                .category(Optional.ofNullable(category))
                .areaId(Optional.ofNullable(areaId))
                .price(priceFilter)
                .build();
    }
}
