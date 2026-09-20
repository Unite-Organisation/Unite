package com.app.prod.facilities.dto;

import com.app.prod.facilities.enums.SlotView;
import com.app.prod.utils.filters.FilterRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FacilityAvailabilityFilterRequest extends FilterRequest {
    @DateTimeFormat(iso = DATE)
    @NotNull private LocalDate day;
    private Boolean myBookingsOnly;
    private Boolean availableOnly;
    private Boolean takenOnly;

    public SlotView view() {
        if (Boolean.TRUE.equals(availableOnly)) {
            return SlotView.FREE;
        }
        if (Boolean.TRUE.equals(takenOnly) || Boolean.TRUE.equals(myBookingsOnly)) {
            return SlotView.TAKEN;
        }
        return SlotView.ALL;
    }
}
