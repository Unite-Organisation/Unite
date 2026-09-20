package com.app.prod.user.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.user.dto.BuildingUserFilterRequest;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.utils.filters.BuildingUserFilter;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class UserFilteringService {

    public BuildingUserFilter prepareFilter(BuildingScope scope, BuildingUserFilterRequest request) {
        return BuildingUserFilter.builder()
                .buildingId(Filter.of(scope.buildingId()))
                .status(Filter.of(effectiveStatus(request)))
                .invitationStatus(Filter.of(request.getInvitationStatus()))
                .createdAt(ComparisonFilter.of(request.getCreatedAt(), request.getCreatedAtModifier()))
                .build();
    }

    private UserStatus effectiveStatus(BuildingUserFilterRequest request) {
        if (request.getInvitationStatus() == null) {
            return request.getStatus();
        }

        if (request.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException(AppError.of(
                    Code.CONFLICTING_FILTERS,
                    "invitationStatus applies to accounts that are not active yet, so it excludes status=ACTIVE"
            ));
        }

        return UserStatus.CREATED;
    }
}
