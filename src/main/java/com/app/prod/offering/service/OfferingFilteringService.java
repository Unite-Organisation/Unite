package com.app.prod.offering.service;

import com.app.prod.area.service.AreaService;
import com.app.prod.offering.enums.OfferingCategory;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.Filter;
import com.app.prod.utils.filters.OfferingFilter;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class OfferingFilteringService {

    private final AreaService areaService;

    public OfferingFilter prepareFilter(
            AppUserRecord user,
            OfferingCategory category,
            BigDecimal price, ComparisonFilter.Modifier modifier
    ) {

        var areaId = areaService.getUserArea(user);

        return OfferingFilter.builder()
                .category(Filter.of(category))
                .areaId(Filter.of(areaId))
                .price(ComparisonFilter.of(price, modifier))
                .build();
    }
}
