package com.app.prod.utils.filters;

import com.app.prod.offering.enums.OfferingCategory;
import lombok.Builder;
import org.jooq.Condition;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static org.jooq.sources.Tables.OFFERING;

@Builder
public class OfferingFilter implements PredicateFilter {
    Filter<OfferingCategory> category;
    Filter<BigDecimal> price;
    Filter<UUID> areaId;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                matchEnum(OFFERING.CATEGORY, category),
                match(OFFERING.AREA_ID, areaId),
                match(OFFERING.PRICE, price)
        );
    }
}
