package com.app.prod.event;

import com.app.prod.event.dto.RankedSlot;
import com.app.prod.event.dto.SlotTally;
import com.app.prod.event.service.SlotRanking;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SlotRankingTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 10, 5, 18, 0);

    @Test
    void shouldPreferTheSlotThatReachesTheThresholdOnPlainYesesAlone() {
        SlotTally wanted = tally(MONDAY, 6, 0);
        SlotTally talkedInto = tally(MONDAY.plusDays(1), 4, 5);

        RankedSlot winner = SlotRanking.qualifying(List.of(talkedInto, wanted), 6).orElseThrow();

        assertThat(winner.slotId())
                .as("a slot nine people can attend still loses to one six people actually want")
                .isEqualTo(wanted.slotId());
    }

    @Test
    void shouldRankBySureYesesWhenNoSlotReachesTheThresholdOnItsOwn() {
        SlotTally fewerSure = tally(MONDAY, 3, 4);
        SlotTally moreSure = tally(MONDAY.plusDays(1), 5, 1);

        RankedSlot winner = SlotRanking.qualifying(List.of(fewerSure, moreSure), 6).orElseThrow();

        assertThat(winner.slotId()).isEqualTo(moreSure.slotId());
        assertThat(winner.qualifies()).isTrue();
    }

    @Test
    void shouldBreakATieOnSureYesesByHeadcount() {
        SlotTally smaller = tally(MONDAY, 4, 1);
        SlotTally bigger = tally(MONDAY.plusDays(1), 4, 3);

        List<RankedSlot> ranked = SlotRanking.rank(List.of(smaller, bigger), 6);

        assertThat(ranked.getFirst().slotId()).isEqualTo(bigger.slotId());
    }

    @Test
    void shouldBreakAFullTieByTheEarliestDate() {
        SlotTally later = tally(MONDAY.plusDays(3), 4, 2);
        SlotTally earlier = tally(MONDAY, 4, 2);

        List<RankedSlot> ranked = SlotRanking.rank(List.of(later, earlier), 6);

        assertThat(ranked.getFirst().slotId()).isEqualTo(earlier.slotId());
    }

    @Test
    void shouldFindNoQualifyingSlotWhileEveryOneIsShort() {
        List<SlotTally> tallies = List.of(tally(MONDAY, 3, 1), tally(MONDAY.plusDays(1), 2, 2));

        assertThat(SlotRanking.qualifying(tallies, 6)).isEmpty();
    }

    @Test
    void shouldStillNameABestSlotWhenNoneReachesTheThreshold() {
        SlotTally weak = tally(MONDAY, 1, 0);
        SlotTally strongest = tally(MONDAY.plusDays(1), 4, 1);

        RankedSlot fallback = SlotRanking.best(List.of(weak, strongest), 6).orElseThrow();

        assertThat(fallback.slotId())
                .as("the deadline has to keep something, threshold or not")
                .isEqualTo(strongest.slotId());
        assertThat(fallback.qualifies()).isFalse();
    }

    @Test
    void shouldKeepSlotsNobodyVotedFor() {
        SlotTally empty = tally(MONDAY.plusDays(1), 0, 0);
        SlotTally voted = tally(MONDAY, 2, 0);

        List<RankedSlot> ranked = SlotRanking.rank(List.of(empty, voted), 6);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.getLast().slotId()).isEqualTo(empty.slotId());
        assertThat(ranked.getLast().chance()).isZero();
    }

    @Test
    void shouldCountAReluctantYesAsHalfTowardsTheChanceShown() {
        assertThat(chanceOf(tally(MONDAY, 3, 0), 6)).isEqualTo(50);
        assertThat(chanceOf(tally(MONDAY, 0, 6), 6)).isEqualTo(50);
        assertThat(chanceOf(tally(MONDAY, 3, 3), 6)).isEqualTo(75);
    }

    @Test
    void shouldNotShowMoreThanAFullChance() {
        assertThat(chanceOf(tally(MONDAY, 20, 20), 6)).isEqualTo(100);
    }

    @Test
    void shouldHandleAnEventWithoutAnySlots() {
        assertThat(SlotRanking.rank(List.of(), 6)).isEmpty();
        assertThat(SlotRanking.qualifying(List.of(), 6)).isEmpty();
        assertThat(SlotRanking.best(List.of(), 6)).isEmpty();
    }

    private static int chanceOf(SlotTally tally, int minAttendees) {
        return SlotRanking.rank(List.of(tally), minAttendees).getFirst().chance();
    }

    private static SlotTally tally(LocalDateTime start, int preferred, int ifNeeded) {
        return new SlotTally(UUID.randomUUID(), start, preferred, ifNeeded);
    }
}
