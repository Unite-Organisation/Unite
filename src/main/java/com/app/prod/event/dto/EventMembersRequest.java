package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.utils.filters.FilterRequest;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EventMembersRequest extends FilterRequest {
    private boolean sortedByCallerMembershipProbability;
    private EventMemberStatus status;
    private EventMemberRole role;
    @Valid
    private DeviceFingerprint device;
}
