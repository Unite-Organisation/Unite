package com.app.prod.interaction;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.PostPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.interaction.dto.InteractionRequest;
import com.app.prod.interaction.dto.InteractionSummary;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.service.InteractionFilteringService;
import com.app.prod.interaction.service.InteractionService;
import com.app.prod.post.dto.PostFilterRequest;
import com.app.prod.post.dto.PostResponse;
import org.jooq.sources.tables.records.PostRecord;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.post.service.PostFilteringService;
import com.app.prod.utils.Pagination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.app.prod.interaction.enums.InteractionEntityType.POST;
import static com.app.prod.interaction.enums.InteractionType.ATTENDING;
import static com.app.prod.post.enums.PostType.ANNOUNCEMENT;
import static com.app.prod.post.enums.PostType.EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class PostInteractionIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private PostPersistenceFactory postPersistenceFactory;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostFilteringService postFilteringService;
    @Autowired
    private InteractionService interactionService;
    @Autowired
    private InteractionFilteringService interactionFilteringService;

    private UUID buildingId;
    private UUID residentId;
    private UUID otherResidentId;
    private BuildingScope residentScope;
    private BuildingScope otherResidentScope;

    @BeforeEach
    void setUp() {
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();

        buildingId = building.getId();
        residentId = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        otherResidentId = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();

        residentScope = TestBuildingScope.of(buildingId, residentId);
        otherResidentScope = TestBuildingScope.of(buildingId, otherResidentId);
    }

    @Test
    void shouldReturnInteractionCountsAlongWithPosts() {
        var eventId = event("Barbecue").getId();

        interactionService.addInteraction(residentScope, attending(eventId));
        interactionService.addInteraction(otherResidentScope, attending(eventId));

        assertThat(interactionsOf(eventId, residentId))
                .containsExactly(new InteractionSummary(ATTENDING, 2, true));

        var thirdUser = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        assertThat(interactionsOf(eventId, thirdUser))
                .containsExactly(new InteractionSummary(ATTENDING, 2, false));
    }

    @Test
    void shouldNotCountTheSameUserTwice() {
        var eventId = event("Cleanup").getId();

        interactionService.addInteraction(residentScope, attending(eventId));
        interactionService.addInteraction(residentScope, attending(eventId));

        assertThat(interactionsOf(eventId, residentId))
                .containsExactly(new InteractionSummary(ATTENDING, 1, true));
    }

    @Test
    void shouldReturnNoInteractionsForPostNobodyReactedTo() {
        var eventId = event("Empty").getId();

        assertThat(interactionsOf(eventId, residentId)).isEmpty();
    }

    @Test
    void shouldRemoveInteraction() {
        var eventId = event("Meeting").getId();

        interactionService.addInteraction(residentScope, attending(eventId));
        interactionService.removeInteraction(residentScope, POST, eventId, ATTENDING);

        assertThat(interactionsOf(eventId, residentId)).isEmpty();
    }

    @Test
    void shouldListUsersThatReacted() {
        var eventId = event("Party").getId();

        interactionService.addInteraction(residentScope, attending(eventId));
        interactionService.addInteraction(otherResidentScope, attending(eventId));

        var filter = interactionFilteringService.prepareFilter(POST, eventId, ATTENDING, null, null);
        var users = interactionService.getInteractions(residentScope, POST, eventId, filter, pagination());

        assertThat(users).extracting(r -> r.user().id()).containsExactlyInAnyOrder(residentId, otherResidentId);
        assertThat(users).extracting(r -> r.user().firstName()).doesNotContainNull();
    }

    @Test
    void shouldRejectAttendingOnAnnouncement() {
        var announcementId = announcement("Water shutdown").getId();

        assertThatThrownBy(() -> interactionService.addInteraction(residentScope, attending(announcementId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectInteractionWithPostFromAnotherBuilding() {
        var eventId = event("Barbecue").getId();

        var otherArea = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var otherBuilding = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(otherArea.getId()).buildAndSave();
        var outsider = userPersistanceFactory.getNewUser().withRandomValues().buildingId(otherBuilding.getId()).buildAndSave().getId();
        var outsiderScope = TestBuildingScope.of(otherBuilding.getId(), outsider);

        assertThatThrownBy(() -> interactionService.addInteraction(outsiderScope, attending(eventId)))
                .isInstanceOf(EntityNotPresentException.class);
    }

    @Test
    void shouldRejectAttendingWhenEventIsFull() {
        var eventId = eventWithLimit("Workshop", 1).getId();

        interactionService.addInteraction(residentScope, attending(eventId));

        assertThatThrownBy(() -> interactionService.addInteraction(otherResidentScope, attending(eventId)))
                .isInstanceOf(BadRequestException.class);
        assertThat(interactionsOf(eventId, residentId))
                .containsExactly(new InteractionSummary(ATTENDING, 1, true));
    }

    @Test
    void shouldNotLetTwoUsersTakeTheSameLastSpot() throws Exception {
        var eventId = eventWithLimit("Workshop", 1).getId();
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> attempts = List.of(residentScope, otherResidentScope).stream()
                    .map(scope -> executor.submit(() -> {
                        start.await();
                        try {
                            interactionService.addInteraction(scope, attending(eventId));
                            return true;
                        } catch (BadRequestException e) {
                            return false;
                        }
                    }))
                    .toList();

            start.countDown();

            long succeeded = attempts.stream().map(PostInteractionIT::get).filter(Boolean::booleanValue).count();
            assertThat(succeeded).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        assertThat(interactionsOf(eventId, residentId))
                .extracting(InteractionSummary::count)
                .containsExactly(1);
    }

    private static <T> T get(Future<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private InteractionRequest attending(UUID entityId) {
        return new InteractionRequest(InteractionEntityType.POST, entityId, InteractionType.ATTENDING);
    }

    private List<InteractionSummary> interactionsOf(UUID postId, UUID userId) {
        var filter = postFilteringService.prepareFilter(TestBuildingScope.of(buildingId, userId), new PostFilterRequest());
        return postRepository.findPosts(userId, pagination(), filter).stream()
                .filter(post -> post.id().equals(postId))
                .map(PostResponse::interactions)
                .findFirst()
                .orElseThrow();
    }

    private Pagination pagination() {
        return Pagination.builder().page(1).pageSize(50).build();
    }

    private PostRecord event(String name) {
        return postPersistenceFactory.getNewPost(EVENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(residentId)
                .buildAndSave();
    }

    private PostRecord eventWithLimit(String name, int maxAttendees) {
        return postPersistenceFactory.getNewPost(EVENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(residentId)
                .maxAttendees(maxAttendees)
                .buildAndSave();
    }

    private PostRecord announcement(String name) {
        return postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(residentId)
                .buildAndSave();
    }
}
