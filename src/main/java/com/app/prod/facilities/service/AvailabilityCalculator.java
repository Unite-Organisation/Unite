package com.app.prod.facilities.service;

import com.app.prod.facilities.dto.FacilityReservation;
import com.app.prod.facilities.dto.FacilitySlot;
import com.app.prod.facilities.enums.SlotView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns the reservations of a single day into the timeline the frontend draws: every reservation is
 * a taken slot, every gap between them is a free one. A day is treated as 00:00 - 24:00, because a
 * facility carries no opening hours.
 */
public class AvailabilityCalculator {

    public static List<FacilitySlot> slotsOf(LocalDate day, SlotView view, List<FacilityReservation> reservations) {
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

        List<FacilitySlot> slots = new ArrayList<>();
        if (view != SlotView.FREE) {
            reservations.stream()
                    .map(reservation -> FacilitySlot.taken(
                            clamp(reservation.startTime(), dayStart, dayEnd),
                            clamp(reservation.endTime(), dayStart, dayEnd),
                            reservation
                    ))
                    .forEach(slots::add);
        }
        if (view != SlotView.TAKEN) {
            slots.addAll(freeSlots(dayStart, dayEnd, reservations));
        }

        slots.sort(Comparator.comparing(FacilitySlot::startTime));
        return slots;
    }

    private static List<FacilitySlot> freeSlots(LocalDateTime dayStart, LocalDateTime dayEnd, List<FacilityReservation> reservations) {
        List<FacilitySlot> freeSlots = new ArrayList<>();

        LocalDateTime freeSince = dayStart;
        for (FacilityReservation reservation : reservations) {
            LocalDateTime takenFrom = clamp(reservation.startTime(), dayStart, dayEnd);
            LocalDateTime takenTo = clamp(reservation.endTime(), dayStart, dayEnd);

            if (takenFrom.isAfter(freeSince)) {
                freeSlots.add(FacilitySlot.free(freeSince, takenFrom));
            }
            if (takenTo.isAfter(freeSince)) {
                freeSince = takenTo;
            }
        }

        if (freeSince.isBefore(dayEnd)) {
            freeSlots.add(FacilitySlot.free(freeSince, dayEnd));
        }
        return freeSlots;
    }

    private static LocalDateTime clamp(LocalDateTime value, LocalDateTime dayStart, LocalDateTime dayEnd) {
        if (value.isBefore(dayStart)) {
            return dayStart;
        }
        if (value.isAfter(dayEnd)) {
            return dayEnd;
        }
        return value;
    }
}
