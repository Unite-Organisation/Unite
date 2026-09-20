package com.app.prod.utils.filters;

import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.repository.EmailDeliveryFields;
import com.app.prod.user.enums.UserStatus;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static com.app.prod.utils.filters.Criteria.required;
import static org.jooq.sources.Tables.APP_USER;

@Builder
public class BuildingUserFilter implements PredicateFilter {
    Filter<UUID> buildingId;
    Filter<UserStatus> status;
    Filter<EmailDeliveryStatus> invitationStatus;
    Filter<LocalDateTime> createdAt;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                required(APP_USER.BUILDING_ID, buildingId),
                matchEnum(APP_USER.STATUS, status),
                matchEnum(EmailDeliveryFields.lastDeliveryStatus(APP_USER.ID, EmailDeliveryType.USER_CREATION), invitationStatus),
                match(APP_USER.CREATED_AT, createdAt)
        );
    }
}
