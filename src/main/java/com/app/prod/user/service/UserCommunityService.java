package com.app.prod.user.service;

import com.app.prod.building.service.BuildingService;
import com.app.prod.user.dto.BasicUserData;
import com.app.prod.user.dto.PotentialContactResponse;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.Pagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCommunityService {

    private final UserRepository userRepository;
    private final BuildingService buildingService;

    public List<PotentialContactResponse> getAllUsersInArea(AppUserRecord userRecord, Pagination pagination){
        //TODO: return only users with who i dont have conversation yet

        var areaId = buildingService.getAreaId(userRecord.getBuildingId());
        return userRepository.getAllUsersInArea(areaId, pagination);
    }

}
