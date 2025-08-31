package com.app.prod.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.time.Clock;

import static com.app.prod.config.Constants.BCRYPT_PASSWORD_ENCODER_STRENGTH;

@Configuration
public class UtilsConfiguration {
    @Bean
    public Clock clock(){
        return Clock.systemDefaultZone();
    }

    @Bean
    public DSLContext dslContext(DataSource dataSource){
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

}
