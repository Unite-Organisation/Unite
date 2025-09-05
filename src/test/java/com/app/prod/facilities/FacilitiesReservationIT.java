package com.app.prod.facilities;

import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import org.jooq.sources.tables.records.UsersRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FacilitiesReservationIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;

    @Test
    void testSomething() {
        UsersRecord user = userPersistanceFactory.getNewUser()
                .withRandomValues()
                .email("randommail")
                .username("username")
                .buildAndSave();

        var i = 0;
    }

}
