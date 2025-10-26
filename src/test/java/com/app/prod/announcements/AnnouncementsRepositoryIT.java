package com.app.prod.announcements;

import com.app.prod.announcements.dto.AnnouncementDto;
import com.app.prod.announcements.dto.AnnouncementResponse;
import com.app.prod.announcements.repository.AnnouncementsRepository;
import com.app.prod.builders.AnnouncementPersistenceFactory;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.utils.Pagination;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class AnnouncementsRepositoryIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private AnnouncementPersistenceFactory announcementPersistenceFactory;

    @Autowired
    private AnnouncementsRepository announcementsRepository;

    @Test
    void shouldReturnAnnouncementsOnlyForMe(){
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        var user = userPersistanceFactory.getNewUser().withRandomValues().buildingId(building.getId()).buildAndSave();

        var manager = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();

        //only for building
        var ann1 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event1")
                .buildingId(building.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //obly for area but user belongs to area
        var ann2 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event2")
                .areaId(area.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        Pagination pagination = Pagination.builder().page(1).pageSize(5).build();

        var result = announcementsRepository.findForUser(user.getId(), pagination);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AnnouncementDto::name).toList()).containsExactlyInAnyOrder("Event1", "Event2");
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

        //for building in my area - but not mine building (SHOULD NOT MATCH)
        var ann1 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event1")
                .buildingId(differentBuilding.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for building in another area (SHOULD NOT MATCH)
        var ann2 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event2")
                .buildingId(farBuilding.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for another area (SHOULD NOT MATCH)
        var ann3 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event3")
                .areaId(differentArea.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for my building
        var ann4 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event4")
                .buildingId(myBuilding.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        //for my area
        var ann5 = announcementPersistenceFactory.getNewAnnouncement()
                .withRandomValues()
                .name("Event5")
                .areaId(myArea.getId())
                .createdBy(manager.getId())
                .buildAndSave();

        Pagination pagination = Pagination.builder().page(1).pageSize(5).build();

        var resultForMyUser = announcementsRepository.findForUser(myUser.getId(), pagination);
        assertThat(resultForMyUser).isNotNull();
        assertThat(resultForMyUser).hasSize(2);
        assertThat(resultForMyUser.stream().map(AnnouncementDto::name).toList()).containsExactlyInAnyOrder("Event4", "Event5");

        var resultForDifferentUser = announcementsRepository.findForUser(differentUserFromFar.getId(), pagination);
        assertThat(resultForDifferentUser).isNotNull();
        assertThat(resultForDifferentUser).hasSize(2);
        assertThat(resultForDifferentUser.stream().map(AnnouncementDto::name).toList()).containsExactlyInAnyOrder("Event2", "Event3");

        var resultForUserFromAnotherCountry = announcementsRepository.findForUser(userFromAnotherCountry.getId(), pagination);
        assertThat(resultForUserFromAnotherCountry).isNotNull();
        assertThat(resultForUserFromAnotherCountry).isEmpty();
    }

    @Test
    void shouldReturnPaginatedResults(){
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        var user = userPersistanceFactory.getNewUser().withRandomValues().buildingId(building.getId()).buildAndSave();
        var manager = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();

        createManyAnnouncements(building.getId(), manager.getId(), 10);
        Pagination pagination = Pagination.builder().page(2).pageSize(3).build();

        var result = announcementsRepository.findForUser(user.getId(), pagination);

        //TODO: assertions
    }

    private void createManyAnnouncements(UUID buildingId, UUID createdBy, int number){
        for(int i = 0; i < number; i++){
            announcementPersistenceFactory.getNewAnnouncement()
                    .withRandomValues()
                    .name("Event" + (i + 1))
                    .buildingId(buildingId)
                    .createdBy(createdBy)
                    .buildAndSave();

        }
    }

}
