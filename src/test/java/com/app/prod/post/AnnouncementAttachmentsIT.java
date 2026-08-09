package com.app.prod.post;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.BuildingsManagersPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.post.service.AnnouncementService;
import com.app.prod.storage.ContextStoragePrefix;
import com.app.prod.storage.InMemoryStorage;
import com.app.prod.storage.StorageKeys;
import com.app.prod.storage.dto.FileResponse;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.PostFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class AnnouncementAttachmentsIT extends IntegrationTest {

    private static final long ONE_MEGABYTE = 1024 * 1024;

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private BuildingsManagersPersistenceFactory buildingsManagersPersistenceFactory;
    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private InMemoryStorage storage;

    private UUID buildingId;
    private UUID managerId;
    private UUID residentId;
    private BuildingScope managerScope;
    private PostFilter filter;

    @BeforeEach
    void setUp() {
        storage.clear();

        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        buildingId = building.getId();
        residentId = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        managerId = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave().getId();

        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(buildingId)
                .managerId(managerId)
                .buildAndSave();

        managerScope = TestBuildingScope.of(buildingId, managerId, UserRole.MANAGER);
        filter = PostFilter.builder()
                .buildingId(buildingId)
                .postType(Optional.of(PostType.ANNOUNCEMENT))
                .createdBy(Optional.empty())
                .visibleFrom(ComparisonFilter.empty())
                .visibleTo(ComparisonFilter.empty())
                .build();
    }

    @Test
    void shouldPersistUploadedFilesAndReturnThemWithSignedUrls() {
        var keys = uploadedKeys(3);
        var confirmedKeys = keys.stream().map(StorageKeys::confirmedKeyOf).toList();

        announcementService.createAnnouncement(request("With photos", keys), managerScope);

        var announcement = onlyAnnouncementFor(residentId);
        assertThat(announcement.files()).hasSize(3);
        assertThat(announcement.files().stream().map(FileResponse::key).toList()).containsExactlyElementsOf(confirmedKeys);
        assertThat(announcement.files()).allSatisfy(file -> {
            assertThat(file.contentType()).isEqualTo("image/jpeg");
            assertThat(file.url()).isEqualTo("http://localhost/fake-download/" + file.key());
        });
    }

    @Test
    void shouldMoveConfirmedFilesOutOfThePendingPrefix() {
        var pendingKey = uploadedKey(managerId, "cat.jpg");

        announcementService.createAnnouncement(request("With photo", List.of(pendingKey)), managerScope);

        assertThat(storage.find(pendingKey)).isEmpty();
        assertThat(storage.find(StorageKeys.confirmedKeyOf(pendingKey))).isPresent();
    }

    @Test
    void shouldRejectKeyThatIsAlreadyConfirmed() {
        var confirmedKey = StorageKeys.build(ContextStoragePrefix.ANNOUNCEMENT, managerId, "cat.jpg");
        storage.put(confirmedKey, "image/jpeg", ONE_MEGABYTE);

        assertThatThrownBy(() -> announcementService.createAnnouncement(request("Reused", List.of(confirmedKey)), managerScope))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldReturnEmptyFileListWhenAnnouncementHasNoAttachments() {
        announcementService.createAnnouncement(request("No photos", null), managerScope);

        assertThat(onlyAnnouncementFor(residentId).files()).isEmpty();
    }

    @Test
    void shouldRejectKeyThatWasNeverUploaded() {
        var key = StorageKeys.buildPending(ContextStoragePrefix.ANNOUNCEMENT, managerId, "ghost.jpg");

        assertThatThrownBy(() -> announcementService.createAnnouncement(request("Ghost", List.of(key)), managerScope))
                .isInstanceOf(EntityNotPresentException.class);
        assertThat(postRepository.findPosts(residentId, pagination(), filter)).isEmpty();
    }

    @Test
    void shouldRejectKeyUploadedByAnotherUser() {
        var foreignKey = uploadedKey(residentId, "someone-else.jpg");

        assertThatThrownBy(() -> announcementService.createAnnouncement(request("Stolen", List.of(foreignKey)), managerScope))
                .isInstanceOf(BadRequestException.class);
        assertThat(postRepository.findPosts(residentId, pagination(), filter)).isEmpty();
    }

    @Test
    void shouldRejectKeyFromAnotherContext() {
        var eventKey = StorageKeys.buildPending(ContextStoragePrefix.EVENT, managerId, "party.jpg");
        storage.put(eventKey, "image/jpeg", ONE_MEGABYTE);

        assertThatThrownBy(() -> announcementService.createAnnouncement(request("Wrong context", List.of(eventKey)), managerScope))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectMoreThanFiveFiles() {
        var keys = uploadedKeys(6);

        assertThatThrownBy(() -> announcementService.createAnnouncement(request("Too many", keys), managerScope))
                .isInstanceOf(BadRequestException.class);
    }

    private List<String> uploadedKeys(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> uploadedKey(managerId, "photo-" + index + ".jpg"))
                .toList();
    }

    private String uploadedKey(UUID ownerId, String fileName) {
        var key = StorageKeys.buildPending(ContextStoragePrefix.ANNOUNCEMENT, ownerId, fileName);
        storage.put(key, "image/jpeg", ONE_MEGABYTE);
        return key;
    }

    private AnnouncementRequest request(String name, List<String> fileKeys) {
        return new AnnouncementRequest(name, "content", null, PostType.ANNOUNCEMENT, null, null, fileKeys);
    }

    private PostResponse onlyAnnouncementFor(UUID userId) {
        var results = postRepository.findPosts(userId, pagination(), filter);
        assertThat(results).hasSize(1);
        return results.getFirst();
    }

    private Pagination pagination() {
        return Pagination.builder().page(1).pageSize(5).build();
    }
}
