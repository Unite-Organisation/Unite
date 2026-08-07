package com.app.prod.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;


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

    /**
     * The DataSource is wrapped, so jOOQ reuses the connection bound to the current Spring
     * transaction instead of taking a fresh one per query. Without the proxy every statement
     * commits on its own and {@code @Transactional} - including row locks - has no effect.
     */
    @Bean
    public DSLContext dslContext(DataSource dataSource){
        return DSL.using(new TransactionAwareDataSourceProxy(dataSource), SQLDialect.POSTGRES);
    }

}
