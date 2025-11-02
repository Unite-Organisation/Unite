package com.app.prod.utils.filters;

import com.app.prod.offering.enums.OfferingCategory;
import lombok.Builder;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.OFFERING;

@Builder
public class OfferingFilter implements PredicateFilter {
    public Optional<OfferingCategory> category;
    public PriceFilter price;
    public Optional<UUID> areaId;

    @Override
    public Condition parseFilterAnd() {
        return combineConditions().stream().reduce(DSL.trueCondition(), Condition::and);
    }

    @Override
    public Condition parseFilterOr() {
        return combineConditions().stream().reduce(DSL.trueCondition(), Condition::or);
    }

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        category.ifPresent(r -> conditionList.add(OFFERING.CATEGORY.eq(r.name())));
        areaId.ifPresent(r -> conditionList.add(OFFERING.AREA_ID.eq(r)));

        switch (price.priceModifier) {
            case LOWER -> price.price.ifPresent(r -> conditionList.add(OFFERING.PRICE.le(r)));
            case HIGHER -> price.price.ifPresent(r -> conditionList.add(OFFERING.PRICE.ge(r)));
            case EQUAL -> price.price.ifPresent(r -> conditionList.add(OFFERING.PRICE.eq(r)));
        }
        return conditionList;
    }
}
