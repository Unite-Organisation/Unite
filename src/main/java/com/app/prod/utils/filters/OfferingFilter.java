package com.app.prod.utils.filters;

import com.app.prod.offering.enums.OfferingCategory;
import lombok.Builder;
import org.jooq.Condition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.OFFERING;

@Builder
public class OfferingFilter implements PredicateFilter {
    public Optional<OfferingCategory> category;
    public ComparisonFilter<BigDecimal> price;
    public Optional<UUID> areaId;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        category.ifPresent(r -> conditionList.add(OFFERING.CATEGORY.eq(r.name())));
        areaId.ifPresent(r -> conditionList.add(OFFERING.AREA_ID.eq(r)));
        price.toCondition(OFFERING.PRICE).ifPresent(conditionList::add);

        return conditionList;
    }
}
