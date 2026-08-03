package com.app.prod.post;

import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.builders.PostPersistenceFactory;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.BuildingsManagersPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.utils.Pagination;

import static com.app.prod.post.enums.PostType.ANNOUNCEMENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.PostFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

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

    private final PostFilter filter = PostFilter.builder()
            .postType(Optional.of(PostType.ANNOUNCEMENT))
            .visibleFrom(ComparisonFilter.empty())
            .visibleTo(ComparisonFilter.empty())
            .build();

    @Test
    void shouldReturnAnnouncementsOnlyForMe(){
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        var user = userPersistanceFactory.getNewUser().withRandomValues().buildingId(building.getId()).buildAndSave();

        var manager = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();
        assignManager(manager.getId(), building.getId());

        //only for building
        var ann1 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event1")
                .buildingId(building.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //obly for area but user belongs to area
        var ann2 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event2")
                .areaId(area.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        Pagination pagination = Pagination.builder().page(1).pageSize(5).build();

        var result = postRepository.findForUser(user.getId(), pagination, filter);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(PostResponse::name).toList()).containsExactlyInAnyOrder("Event1", "Event2");
    }

    @Test
    void shouldReturnOnlyAnnouncementsForRelatedUser() {
        var myArea = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var differentArea = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var areaFromAnotherCountry = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();

        var myBuilding = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(myArea.getId()).buildAndSave();
        var differentBuilding = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(myArea.getId()).buildAndSave();
        var farBuilding = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(differentArea.getId()).buildAndSave();
        var buildingFromAnotherCountry = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(areaFromAnotherCountry.getId()).buildAndSave();

        var myUser = userPersistanceFactory.getNewUser().withRandomValues().buildingId(myBuilding.getId()).buildAndSave();
        var differentUserFromFar = userPersistanceFactory.getNewUser().withRandomValues().buildingId(farBuilding.getId()).buildAndSave();
        var userFromAnotherCountry = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingFromAnotherCountry.getId()).buildAndSave();

        var manager = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();
        assignManager(manager.getId(), myBuilding.getId());
        assignManager(manager.getId(), differentBuilding.getId());
        assignManager(manager.getId(), farBuilding.getId());
        assignManager(manager.getId(), buildingFromAnotherCountry.getId());

        //for building in my area - but not mine building (SHOULD NOT MATCH)
        var ann1 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event1")
                .buildingId(differentBuilding.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for building in another area (SHOULD NOT MATCH)
        var ann2 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event2")
                .buildingId(farBuilding.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for another area (SHOULD NOT MATCH)
        var ann3 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event3")
                .areaId(differentArea.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for my building
        var ann4 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event4")
                .buildingId(myBuilding.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for my area
        var ann5 = postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                .withRandomValues()
                .name("Event5")
                .areaId(myArea.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        Pagination pagination = Pagination.builder().page(1).pageSize(5).build();

        var resultForMyUser = postRepository.findForUser(myUser.getId(), pagination, filter);
        assertThat(resultForMyUser).isNotNull();
        assertThat(resultForMyUser).hasSize(2);
        assertThat(resultForMyUser.stream().map(PostResponse::name).toList()).containsExactlyInAnyOrder("Event4", "Event5");

        var resultForDifferentUser = postRepository.findForUser(differentUserFromFar.getId(), pagination, filter);
        assertThat(resultForDifferentUser).isNotNull();
        assertThat(resultForDifferentUser).hasSize(2);
        assertThat(resultForDifferentUser.stream().map(PostResponse::name).toList()).containsExactlyInAnyOrder("Event2", "Event3");

        var resultForUserFromAnotherCountry = postRepository.findForUser(userFromAnotherCountry.getId(), pagination, filter);
        assertThat(resultForUserFromAnotherCountry).isNotNull();
        assertThat(resultForUserFromAnotherCountry).isEmpty();
    }

    @Test
    void shouldReturnPaginatedResults(){
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        var user = userPersistanceFactory.getNewUser().withRandomValues().buildingId(building.getId()).buildAndSave();
        var manager = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();
        assignManager(manager.getId(), building.getId());

        createManyAnnouncements(building.getId(), manager.getId(), 10);
        Pagination pagination = Pagination.builder().page(2).pageSize(3).build();

        var result = postRepository.findForUser(user.getId(), pagination, filter);

        //TODO: assertions
    }

    private void assignManager(UUID managerId, UUID buildingId){
        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(buildingId)
                .managerId(managerId)
                .buildAndSave();
    }

    private void createManyAnnouncements(UUID buildingId, UUID createdBy, int number){
        for(int i = 0; i < number; i++){
            postPersistenceFactory.getNewPost(ANNOUNCEMENT)
                    .withRandomValues()
                    .name("Event" + (i + 1))
                    .buildingId(buildingId)
                    .createdBy(createdBy)
                    .buildAndSave();

        }
    }

}
