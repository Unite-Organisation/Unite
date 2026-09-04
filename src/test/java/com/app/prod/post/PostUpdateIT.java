package com.app.prod.post;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.BuildingsManagersPersistenceFactory;
import com.app.prod.builders.PostPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.repository.InteractionRepository;
import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.post.service.AnnouncementService;
import com.app.prod.post.service.EventService;
import com.app.prod.user.enums.UserRole;
import org.jooq.sources.tables.records.PostRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.app.prod.post.enums.PostType.ANNOUNCEMENT;
import static com.app.prod.post.enums.PostType.EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class PostUpdateIT extends IntegrationTest {

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
    private InteractionRepository interactionRepository;
    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private EventService eventService;
    @Autowired
    private Clock clock;

    private UUID buildingId;
    private UUID otherBuildingId;
    private UUID managerId;
    private BuildingScope managerScope;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
        otherBuildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();

        managerId = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave().getId();

        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(buildingId)
                .managerId(managerId)
                .buildAndSave();

        managerScope = TestBuildingScope.of(buildingId, managerId, UserRole.MANAGER);
    }

    @Test
    void shouldUpdateAnnouncementFields() {
        var announcement = announcement("Before").buildAndSave();

        announcementService.updateAnnouncement(
                announcement.getId(),
                announcementRequest("After", now.minusDays(1), now.plusDays(30)),
                managerScope
        );

        var updated = reload(announcement.getId());
        assertThat(updated.getName()).isEqualTo("After");
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getVisibleFrom()).isEqualTo(now.minusDays(1));
        assertThat(updated.getVisibleTo()).isEqualTo(now.plusDays(30));
    }

    @Test
    void shouldNotChangeOwnershipOfUpdatedAnnouncement() {
        var announcement = announcement("Before").buildAndSave();

        announcementService.updateAnnouncement(
                announcement.getId(),
                announcementRequest("After", now, now.plusDays(1)),
                managerScope
        );

        var updated = reload(announcement.getId());
        assertThat(updated.getBuildingId()).isEqualTo(buildingId);
        assertThat(updated.getCreatedBy()).isEqualTo(managerId);
        assertThat(updated.getCreatedAt()).isEqualTo(announcement.getCreatedAt());
        assertThat(updated.getPostType()).isEqualTo(ANNOUNCEMENT.name());
    }

    @Test
    void shouldRejectAnnouncementUpdateCarryingAnotherPostType() {
        var announcement = announcement("Before").buildAndSave();
        var request = new AnnouncementRequest("After", "Updated content", null, EVENT, now, now.plusDays(1), null);

        assertThatThrownBy(() -> announcementService.updateAnnouncement(announcement.getId(), request, managerScope))
                .isInstanceOf(BadRequestException.class);

        assertThat(reload(announcement.getId()).getName()).isEqualTo("Before");
    }

    @Test
    void shouldRejectUpdateOfAnnouncementThatDoesNotExist() {
        var request = announcementRequest("After", now, now.plusDays(1));

        assertThatThrownBy(() -> announcementService.updateAnnouncement(UUID.randomUUID(), request, managerScope))
                .isInstanceOf(EntityNotPresentException.class);
    }

    @Test
    void shouldRejectUpdateOfAnnouncementFromAnotherBuilding() {
        var foreign = announcement("Foreign").buildingId(otherBuildingId).buildAndSave();
        var request = announcementRequest("Hijacked", now, now.plusDays(1));

        assertThatThrownBy(() -> announcementService.updateAnnouncement(foreign.getId(), request, managerScope))
                .isInstanceOf(EntityNotPresentException.class);

        assertThat(reload(foreign.getId()).getName()).isEqualTo("Foreign");
    }

    @Test
    void shouldRejectAnnouncementVisibleFromAfterVisibleTo() {
        var announcement = announcement("Before").buildAndSave();
        var request = announcementRequest("After", now.plusDays(10), now.plusDays(1));

        assertThatThrownBy(() -> announcementService.updateAnnouncement(announcement.getId(), request, managerScope))
                .isInstanceOf(BadRequestException.class);

        assertThat(reload(announcement.getId()).getName()).isEqualTo("Before");
    }

    @Test
    void shouldUpdateEventFields() {
        var event = event("Before").maxAttendees(10).buildAndSave();

        eventService.updateEvent(
                event.getId(),
                eventRequest("After", now.plusDays(2), now.plusDays(3), "Rooftop", "https://meet.test/after", 10),
                managerScope
        );

        var updated = reload(event.getId());
        assertThat(updated.getName()).isEqualTo("After");
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getStartDateTime()).isEqualTo(now.plusDays(2));
        assertThat(updated.getEndDateTime()).isEqualTo(now.plusDays(3));
        assertThat(updated.getLocationName()).isEqualTo("Rooftop");
        assertThat(updated.getOnlineUrl()).isEqualTo("https://meet.test/after");
        assertThat(updated.getMaxAttendees()).isEqualTo(10);
    }

    @Test
    void shouldRejectEventUpdateCarryingAnotherPostType() {
        var event = event("Before").maxAttendees(10).buildAndSave();
        var request = new EventRequest("After", "Updated content", null, ANNOUNCEMENT, now, now.plusDays(1), null, null, 10, null);

        assertThatThrownBy(() -> eventService.updateEvent(event.getId(), request, managerScope))
                .isInstanceOf(BadRequestException.class);

        assertThat(reload(event.getId()).getName()).isEqualTo("Before");
    }

    @Test
    void shouldRejectUpdateOfEventThatDoesNotExist() {
        var request = eventRequest("After", now, now.plusDays(1), "Rooftop", null, 10);

        assertThatThrownBy(() -> eventService.updateEvent(UUID.randomUUID(), request, managerScope))
                .isInstanceOf(EntityNotPresentException.class);
    }

    @Test
    void shouldRejectUpdateOfEventFromAnotherBuilding() {
        var foreign = event("Foreign").buildingId(otherBuildingId).maxAttendees(10).buildAndSave();
        var request = eventRequest("Hijacked", now, now.plusDays(1), "Rooftop", null, 10);

        assertThatThrownBy(() -> eventService.updateEvent(foreign.getId(), request, managerScope))
                .isInstanceOf(EntityNotPresentException.class);

        assertThat(reload(foreign.getId()).getName()).isEqualTo("Foreign");
    }


    @Test
    void shouldRaiseMaxAttendeesAboveCurrentAttendance() {
        var event = event("Barbecue").maxAttendees(2).buildAndSave();
        attend(event.getId(), 2);

        eventService.updateEvent(event.getId(), eventRequest("Barbecue", now, now.plusDays(1), "Rooftop", null, 5), managerScope);

        assertThat(reload(event.getId()).getMaxAttendees()).isEqualTo(5);
    }

    @Test
    void shouldLowerMaxAttendeesDownToCurrentAttendance() {
        var event = event("Barbecue").maxAttendees(10).buildAndSave();
        attend(event.getId(), 3);

        eventService.updateEvent(event.getId(), eventRequest("Barbecue", now, now.plusDays(1), "Rooftop", null, 3), managerScope);

        assertThat(reload(event.getId()).getMaxAttendees()).isEqualTo(3);
    }

    @Test
    void shouldRejectLoweringMaxAttendeesBelowCurrentAttendance() {
        var event = event("Barbecue").maxAttendees(10).buildAndSave();
        attend(event.getId(), 3);

        assertThatThrownBy(() -> eventService.updateEvent(
                event.getId(),
                eventRequest("Barbecue", now, now.plusDays(1), "Rooftop", null, 2),
                managerScope
        )).isInstanceOf(BadRequestException.class);

        assertThat(reload(event.getId()).getMaxAttendees()).isEqualTo(10);
    }

    @Test
    void shouldCountEveryAttendeeWhenLoweringMaxAttendees() {
        var event = event("Barbecue").maxAttendees(100).buildAndSave();
        attend(event.getId(), 25);

        assertThatThrownBy(() -> eventService.updateEvent(
                event.getId(),
                eventRequest("Barbecue", now, now.plusDays(1), "Rooftop", null, 20),
                managerScope
        )).isInstanceOf(BadRequestException.class);

        assertThat(reload(event.getId()).getMaxAttendees()).isEqualTo(100);
    }

    @Test
    void shouldSetMaxAttendeesOnEventThatHadNoLimit() {
        var event = event("Barbecue").buildAndSave();

        eventService.updateEvent(event.getId(), eventRequest("Barbecue", now, now.plusDays(1), "Rooftop", null, 5), managerScope);

        assertThat(reload(event.getId()).getMaxAttendees()).isEqualTo(5);
    }

    private PostPersistenceFactory.Builder announcement(String name) {
        return postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(managerId)
                .createdAt(now);
    }

    private PostPersistenceFactory.Builder event(String name) {
        return postPersistenceFactory.getNewPost(EVENT)
                .withRandomValues()
                .name(name)
                .buildingId(buildingId)
                .createdBy(managerId)
                .createdAt(now)
                .startDateTime(now.plusDays(1))
                .endDateTime(now.plusDays(1).plusHours(2));
    }

    private AnnouncementRequest announcementRequest(String name, LocalDateTime visibleFrom, LocalDateTime visibleTo) {
        return new AnnouncementRequest(name, "Updated content", null, ANNOUNCEMENT, visibleFrom, visibleTo, null);
    }

    private EventRequest eventRequest(String name, LocalDateTime startDate, LocalDateTime endDate, String location, String onlineUrl, Integer maxAttendees) {
        return new EventRequest(name, "Updated content", null, EVENT, startDate, endDate, location, onlineUrl, maxAttendees, null);
    }

    private void attend(UUID eventId, int attendees) {
        List<UUID> userIds = IntStream.range(0, attendees)
                .mapToObj(index -> userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId())
                .toList();

        userIds.forEach(userId ->
                interactionRepository.add(userId, InteractionEntityType.POST, eventId, InteractionType.ATTENDING, now));
    }

    private PostRecord reload(UUID postId) {
        return postRepository.findById(postId).orElseThrow();
    }
}
