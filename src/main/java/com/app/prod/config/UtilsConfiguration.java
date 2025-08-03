package com.app.prod.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

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
