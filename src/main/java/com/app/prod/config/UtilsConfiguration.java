package com.app.prod.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static com.app.prod.config.Constants.BCRYPT_PASSWORD_ENCODER_STRENGTH;

@Configuration
public class UtilsConfiguration {

    @Bean
    @Profile("!test")
    public Clock clock(){
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile("test")
    public MutableClock mutableClock(){
        return new MutableClock(Instant.now(), ZoneId.systemDefault());
    }

    @Bean
    @Primary
    @Profile("test")
    public Clock testClock(MutableClock mutableClock) {
        return mutableClock;
    }

    @Bean
    public DSLContext dslContext(DataSource dataSource){
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

}
