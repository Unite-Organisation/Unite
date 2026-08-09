package com.app.prod.post;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.BuildingsManagersPersistenceFactory;
import com.app.prod.builders.PostPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.repository.PostRepository;
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

import static com.app.prod.post.enums.PostType.ANNOUNCEMENT;
import static com.app.prod.post.enums.PostType.EVENT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AnnouncementRepositoryIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private PostPersistenceFactory postPersistenceFactory;
    @Autowired
    private BuildingsManagersPersistenceFactory buildingsManagersPersistenceFactory;

    @Autowired
    private PostRepository postRepository;

    private UUID buildingId;
    private UUID otherBuildingId;
    private UUID residentId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var otherArea = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();

        buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
        otherBuildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(otherArea.getId()).buildAndSave().getId();

        residentId = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        managerId = userPersistanceFactory.getNewUser().withRandomValues().userRole(UserRole.MANAGER).buildAndSave().getId();

        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(buildingId)
                .managerId(managerId)
                .buildAndSave();
    }

    @Test
    void shouldReturnOnlyPostsOfTheBuildingInScope(){
        announcement("Mine", buildingId);
        announcement("Somewhere else", otherBuildingId);

        var scope = TestBuildingScope.of(buildingId, residentId);

        assertThat(namesFor(scope)).containsExactly("Mine");
    }

    /* The building decides what is returned, not who is asking - access is settled before the query. */
    @Test
    void shouldReturnTheSamePostsToManagerAndResidentOfOneBuilding(){
        announcement("Water shutdown", buildingId);

        var residentScope = TestBuildingScope.of(buildingId, residentId);
        var managerScope = TestBuildingScope.of(buildingId, managerId, UserRole.MANAGER);

        assertThat(namesFor(residentScope)).containsExactly("Water shutdown");
        assertThat(namesFor(managerScope)).containsExactly("Water shutdown");
    }

    @Test
    void shouldReturnPostsOfEveryManagedBuildingSeparately(){
        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(otherBuildingId)
                .managerId(managerId)
                .buildAndSave();

        announcement("First building", buildingId);
        announcement("Second building", otherBuildingId);

        assertThat(namesFor(TestBuildingScope.of(buildingId, managerId, UserRole.MANAGER))).containsExactly("First building");
        assertThat(namesFor(TestBuildingScope.of(otherBuildingId, managerId, UserRole.MANAGER))).containsExactly("Second building");
    }

    @Test
    void shouldNarrowResultsToRequestedPostType(){
        announcement("Announcement", buildingId);
        postPersistenceFactory.getNewPost(EVENT)
                .withRandomValues()
                .name("Event")
                .buildingId(buildingId)
                .createdBy(managerId)
                .buildAndSave();

        var scope = TestBuildingScope.of(buildingId, residentId);

        assertThat(namesFor(scope)).containsExactly("Announcement");
        assertThat(namesFor(scope, filter(scope, EVENT))).containsExactly("Event");
    }

    @Test
    void shouldReturnPaginatedResults(){
        createManyAnnouncements(10);

        var scope = TestBuildingScope.of(buildingId, residentId);
        var secondPage = postRepository.findPosts(residentId, Pagination.builder().page(2).pageSize(3).build(), filter(scope, ANNOUNCEMENT));

        assertThat(secondPage).hasSize(3);
    }

    private void announcement(String name, UUID postBuildingId){
        postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name(name)
                .buildingId(postBuildingId)
                .createdBy(managerId)
                .buildAndSave();
    }

    private void createManyAnnouncements(int number){
        for(int i = 0; i < number; i++){
            announcement("Event" + (i + 1), buildingId);
        }
    }

    private PostFilter filter(BuildingScope scope, PostType postType){
        return PostFilter.builder()
                .buildingId(scope.buildingId())
                .postType(Optional.of(postType))
                .visibleFrom(ComparisonFilter.empty())
                .visibleTo(ComparisonFilter.empty())
                .build();
    }

    private List<String> namesFor(BuildingScope scope){
        return namesFor(scope, filter(scope, ANNOUNCEMENT));
    }

    private List<String> namesFor(BuildingScope scope, PostFilter filter){
        var pagination = Pagination.builder().page(1).pageSize(5).build();
        return postRepository.findPosts(scope.userId(), pagination, filter).stream()
                .map(PostResponse::name)
                .toList();
    }
}
