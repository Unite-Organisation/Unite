package com.app.prod.issues;

import com.app.prod.builders.*;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.config.IntegrationTest;
import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.enums.IssueObject;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.issues.repository.NotificationRepository;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.JwtTestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.sources.tables.records.*;

import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class IssueIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private BuildingsManagersPersistenceFactory buildingsManagersPersistenceFactory;
    @Autowired
    private FacilityPersistanceFactory facilityPersistanceFactory;
    @Autowired
    private PollPersistenceFactory pollPersistenceFactory;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTestHelper jwtTestHelper;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private BuildingsManagersRepository buildingsManagersRepository;
    @Autowired
    private Clock clock;

    private BuildingsRecord building;
    private UsersRecord user;
    private UsersRecord manager;
    private AreasRecord area;

    @Test
    void buildingIssueTest() throws Exception {
        createUserInBuilding();

        String token = jwtTestHelper.generateTokenForUser(user, UserRole.RESIDENT);

        IssueRequest issueRequest = new IssueRequest(
                "Leak",
                "Massive leak in the building",
                IssuePriority.HIGH,
                IssueObject.BUILDING,
                null,
                building.getId(),
                null,
                null,
                false
        );

        mvc.perform(post("/issue")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var issues = issueRepository.findAll().stream()
                .filter(issue -> issue.getCreatedBy().equals(user.getId()))
                .toList();
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getBuildingId()).isEqualTo(building.getId());
        assertThat(issues.get(0).getCreatedBy()).isEqualTo(user.getId());
        assertThat(issues.get(0).getIssueObject()).isEqualTo(IssueObject.BUILDING.name());
        assertThat(issues.get(0).getPriority()).isEqualTo(IssuePriority.HIGH.name());
        assertThat(issues.get(0).getStatus()).isEqualTo(IssueProcessingStatus.SUBMITTED.name());
        assertThat(issues.get(0).getTitle()).isEqualTo(issueRequest.title());
        assertThat(issues.get(0).getDescription()).isEqualTo(issueRequest.description());
        assertThat(issues.get(0).getNotifyEveryone()).isEqualTo(issueRequest.notifyEveryone());
        assertThat(issues.get(0).getAreaId()).isNull();
        assertThat(issues.get(0).getFacilityId()).isNull();
        assertThat(issues.get(0).getPollId()).isNull();

        var notifications = notificationRepository.findAll().stream()
                .filter(notification -> notification.getIssueId().equals(issues.get(0).getId()))
                .toList();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getIssueId()).isEqualTo(issues.get(0).getId());
        assertThat(notifications.get(0).getRecipientId()).isEqualTo(buildingsManagersRepository.fetchManagerIdForBuilding(building.getId()));
        assertThat(notifications.get(0).getSeenAt()).isNull();
    }

    @Test
    void areaIssueTest() throws Exception {
        createUserInBuilding();

        String token = jwtTestHelper.generateTokenForUser(user, UserRole.RESIDENT);

        IssueRequest issueRequest = new IssueRequest(
                "Area Problem",
                "Common area needs maintenance",
                IssuePriority.MEDIUM,
                IssueObject.AREA,
                area.getId(),
                null,
                null,
                null,
                false
        );

        mvc.perform(post("/issue")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var issues = issueRepository.findAll().stream()
                .filter(issue -> issue.getCreatedBy().equals(user.getId()))
                .toList();
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getAreaId()).isEqualTo(area.getId());
        assertThat(issues.get(0).getCreatedBy()).isEqualTo(user.getId());
        assertThat(issues.get(0).getIssueObject()).isEqualTo(IssueObject.AREA.name());
        assertThat(issues.get(0).getPriority()).isEqualTo(IssuePriority.MEDIUM.name());
        assertThat(issues.get(0).getStatus()).isEqualTo(IssueProcessingStatus.SUBMITTED.name());
        assertThat(issues.get(0).getBuildingId()).isNull();
        assertThat(issues.get(0).getFacilityId()).isNull();
        assertThat(issues.get(0).getPollId()).isNull();

        // Area issues don't create notifications (no area manager yet)
        var notifications = notificationRepository.findAll().stream()
                .filter(notification -> notification.getIssueId().equals(issues.get(0).getId()))
                .toList();
        assertThat(notifications).isEmpty();
    }

    @Test
    void facilityIssueTest() throws Exception {
        createUserInBuilding();

        FacilitiesRecord facility = facilityPersistanceFactory.getNewFacility()
                .withRandomValues()
                .buildingId(building.getId())
                .buildAndSave();

        String token = jwtTestHelper.generateTokenForUser(user, UserRole.RESIDENT);

        IssueRequest issueRequest = new IssueRequest(
                "Broken Equipment",
                "Gym equipment is broken",
                IssuePriority.LOW,
                IssueObject.FACILITY,
                null,
                null,
                facility.getId(),
                null,
                false
        );

        mvc.perform(post("/issue")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var issues = issueRepository.findAll().stream()
                .filter(issue -> issue.getCreatedBy().equals(user.getId()))
                .toList();
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getFacilityId()).isEqualTo(facility.getId());
        assertThat(issues.get(0).getCreatedBy()).isEqualTo(user.getId());
        assertThat(issues.get(0).getIssueObject()).isEqualTo(IssueObject.FACILITY.name());
        assertThat(issues.get(0).getPriority()).isEqualTo(IssuePriority.LOW.name());
        assertThat(issues.get(0).getStatus()).isEqualTo(IssueProcessingStatus.SUBMITTED.name());
        assertThat(issues.get(0).getAreaId()).isNull();
        assertThat(issues.get(0).getBuildingId()).isNull();
        assertThat(issues.get(0).getPollId()).isNull();

        // Facility issues notify the building manager
        var notifications = notificationRepository.findAll().stream()
                .filter(notification -> notification.getIssueId().equals(issues.get(0).getId()))
                .toList();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getIssueId()).isEqualTo(issues.get(0).getId());
        assertThat(notifications.get(0).getRecipientId()).isEqualTo(manager.getId());
        assertThat(notifications.get(0).getSeenAt()).isNull();
    }

    @Test
    void pollIssueTest() throws Exception {
        createUserInBuilding();

        PollsRecord poll = pollPersistenceFactory.getNewPoll()
                .withRandomValues()
                .buildingId(building.getId())
                .createdBy(manager.getId())
                .finished(true)
                .endTime(LocalDateTime.now(clock).minusDays(1))
                .buildAndSave();

        String token = jwtTestHelper.generateTokenForUser(user, UserRole.RESIDENT);

        IssueRequest issueRequest = new IssueRequest(
                "Poll Issue",
                "There's a problem with the poll results",
                IssuePriority.HIGH,
                IssueObject.POLL,
                null,
                null,
                null,
                poll.getId(),
                false
        );

        mvc.perform(post("/issue")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var issues = issueRepository.findAll().stream()
                .filter(issue -> issue.getCreatedBy().equals(user.getId()))
                .toList();
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getPollId()).isEqualTo(poll.getId());
        assertThat(issues.get(0).getCreatedBy()).isEqualTo(user.getId());
        assertThat(issues.get(0).getIssueObject()).isEqualTo(IssueObject.POLL.name());
        assertThat(issues.get(0).getPriority()).isEqualTo(IssuePriority.HIGH.name());
        assertThat(issues.get(0).getStatus()).isEqualTo(IssueProcessingStatus.SUBMITTED.name());
        assertThat(issues.get(0).getAreaId()).isNull();
        assertThat(issues.get(0).getBuildingId()).isNull();
        assertThat(issues.get(0).getFacilityId()).isNull();

        // Poll issues notify the poll creator (manager)
        var notifications = notificationRepository.findAll().stream()
                .filter(notification -> notification.getIssueId().equals(issues.get(0).getId()))
                .toList();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getIssueId()).isEqualTo(issues.get(0).getId());
        assertThat(notifications.get(0).getRecipientId()).isEqualTo(manager.getId());
        assertThat(notifications.get(0).getSeenAt()).isNull();
    }

    private void createUserInBuilding(){
        area = areaPersistenceFactory.getNewArea()
                .withRandomValues()
                .buildAndSave();
        building = buildingPersistenceFactory.getNewBuilding()
                .withRandomValues()
                .areaId(area.getId())
                .buildAndSave();
        user = userPersistanceFactory.getNewUser()
                .withRandomValues()
                .buildingId(building.getId())
                .buildAndSave();
        manager = userPersistanceFactory.getNewUser()
                .withRandomValues()
                .userRole(UserRole.MANAGER)
                .buildAndSave();

        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(building.getId())
                .managerId(manager.getId())
                .buildAndSave();
    }

}
