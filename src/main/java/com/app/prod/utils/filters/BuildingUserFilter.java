package com.app.prod.utils.filters;

import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.repository.EmailDeliveryFields;
import com.app.prod.user.enums.UserStatus;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.APP_USER;

@Builder
public class BuildingUserFilter implements PredicateFilter {
    UUID buildingId;
    Optional<UserStatus> status;
    Optional<EmailDeliveryStatus> invitationStatus;
    ComparisonFilter<LocalDateTime> createdAt;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        conditionList.add(APP_USER.BUILDING_ID.eq(buildingId));
        status.ifPresent(value -> conditionList.add(APP_USER.STATUS.eq(value.name())));
        invitationStatus.ifPresent(value -> conditionList.add(
                EmailDeliveryFields.lastDeliveryStatus(APP_USER.ID, EmailDeliveryType.USER_CREATION).eq(value.name())
        ));
        createdAt.toCondition(APP_USER.CREATED_AT).ifPresent(conditionList::add);

        return conditionList;
    }
}
