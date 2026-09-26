package com.app.prod.event.service;

import com.app.prod.event.dto.RankedSlot;
import com.app.prod.event.dto.SlotTally;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SlotRanking {
    private static final double IF_NEEDED_WEIGHT = 0.5;

    private SlotRanking() {
    }

    public static List<RankedSlot> rank(List<SlotTally> tallies, int minAttendees) {
        return tallies.stream()
                .sorted(order(minAttendees))
                .map(tally -> new RankedSlot(tally, qualifies(tally, minAttendees), chance(tally, minAttendees)))
                .toList();
    }

    /** The slot that can carry the event right now, if any can. */
    public static Optional<RankedSlot> qualifying(List<SlotTally> tallies, int minAttendees) {
        return rank(tallies, minAttendees).stream()
                .filter(RankedSlot::qualifies)
                .findFirst();
    }

    /** The best there is, threshold or not - what the deadline keeps when nothing reached it. */
    public static Optional<RankedSlot> best(List<SlotTally> tallies, int minAttendees) {
        return rank(tallies, minAttendees).stream().findFirst();
    }

    static Comparator<SlotTally> order(int minAttendees) {
        return Comparator
                .comparingInt((SlotTally tally) -> carriedOnItsOwn(tally, minAttendees) ? 1 : 0).reversed()
                .thenComparing(Comparator.comparingInt(SlotTally::preferred).reversed())
                .thenComparing(Comparator.comparingInt(SlotTally::total).reversed())
                .thenComparing(SlotTally::startDateTime)
                .thenComparing(SlotTally::slotId);
    }

    private static boolean carriedOnItsOwn(SlotTally tally, int minAttendees) {
        return tally.preferred() >= minAttendees;
    }

    private static boolean qualifies(SlotTally tally, int minAttendees) {
        return tally.total() >= minAttendees;
    }

    private static int chance(SlotTally tally, int minAttendees) {
        if (minAttendees <= 0) {
            return 100;
        }
        double weighted = tally.preferred() + IF_NEEDED_WEIGHT * tally.ifNeeded();
        return (int) Math.min(100, Math.round(100.0 * weighted / minAttendees));
    }
}
