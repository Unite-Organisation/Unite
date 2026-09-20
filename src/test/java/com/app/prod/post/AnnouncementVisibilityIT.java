package com.app.prod.post;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.BuildingsManagersPersistenceFactory;
import com.app.prod.builders.PostPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.post.dto.PostFilterRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.post.service.PostFilteringService;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.PostFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.post.enums.PostType.ANNOUNCEMENT;
import static com.app.prod.post.enums.PostType.EVENT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AnnouncementVisibilityIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private BuildingsManagersPersistenceFactory buildingsManagersPersistenceFactory;
    @Autowired
    private PostPersistenceFactory postPersistenceFactory;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostFilteringService postFilteringService;
    @Autowired
    private MutableClock clock;

    private UUID buildingId;
    private UUID residentId;
    private UUID managerId;
    private BuildingScope scope;
    private Instant clockAtStart;

    @BeforeEach
    void setUp() {
        clockAtStart = clock.instant();

        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();

        buildingId = building.getId();
        residentId = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        managerId = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave().getId();

        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(buildingId)
                .managerId(managerId)
                .buildAndSave();

        scope = TestBuildingScope.of(buildingId, residentId);
    }

    @AfterEach
    void restoreClock() {
        clock.setInstant(clockAtStart);
    }

    @Test
    void shouldHideAnnouncementThatAlreadyEnded() {
        var now = LocalDateTime.now(clock);

        announcement("Expired").visibleFrom(now.minusDays(10)).visibleTo(now.minusDays(1)).buildAndSave();

        assertThat(defaultNames()).isEmpty();
    }

    @Test
    void shouldHideAnnouncementScheduledForTheFutureUntilItStarts() {
        var now = LocalDateTime.now(clock);

        announcement("Scheduled").visibleFrom(now.plusDays(7)).visibleTo(now.plusDays(14)).buildAndSave();

        assertThat(defaultNames()).isEmpty();

        clock.advance(Duration.ofDays(8));

        assertThat(defaultNames()).containsExactly("Scheduled");
    }

    @Test
    void shouldStillReturnEventThatAlreadyHappened() {
        var now = LocalDateTime.now(clock);

        event("Finished")
                .startDateTime(now.minusDays(4))
                .endDateTime(now.minusDays(3))
                .visibleFrom(now.minusDays(6))
                .visibleTo(now.plusDays(10))
                .buildAndSave();

        assertThat(defaultNames()).containsExactly("Finished");
    }

    @Test
    void shouldReturnEndedAnnouncementWhenExplicitDateFilterIsGiven() {
        var now = LocalDateTime.now(clock);

        announcement("Expired").visibleFrom(now.minusDays(10)).visibleTo(now.minusDays(1)).buildAndSave();
        announcement("Active").visibleFrom(now.minusDays(1)).visibleTo(now.plusDays(1)).buildAndSave();

        var request = PostFilterRequest.builder()
                .visibleTo(now)
                .visibleToModifier(ComparisonFilter.Modifier.LESS_OR_EQUAL_THAN)
                .build();
        var filter = postFilteringService.prepareFilter(scope, request);

        assertThat(namesFor(filter)).containsExactly("Expired");
    }

    private PostPersistenceFactory.Builder announcement(String name) {
        return postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(managerId);
    }

    private PostPersistenceFactory.Builder event(String name) {
        return postPersistenceFactory.getNewPost(EVENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(managerId);
    }

    private List<String> defaultNames() {
        return namesFor(postFilteringService.prepareFilter(scope, new PostFilterRequest()));
    }

    private List<String> namesFor(PostFilter filter) {
        var pagination = Pagination.builder().page(1).pageSize(5).build();
        return postRepository.findPosts(residentId, pagination, filter).stream()
                .map(PostResponse::name)
                .toList();
    }

}
