package com.app.prod.user.dto;

import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.FilterRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BuildingUserFilterRequest extends FilterRequest {
    private UserStatus status;
    private EmailDeliveryStatus invitationStatus;
    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime createdAt;
    private ComparisonFilter.Modifier createdAtModifier;
}
