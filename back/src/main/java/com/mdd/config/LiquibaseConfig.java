package com.mdd.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {

    @Bean
    public SpringLiquibase liquibase(
            DataSource dataSource,
            @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.sql}") String changeLog,
            @Value("${spring.liquibase.contexts:}") String contexts,
            @Value("${spring.liquibase.enabled:true}") boolean enabled
    ) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        liquibase.setShouldRun(enabled);
        if (!contexts.isBlank()) {
            liquibase.setContexts(contexts);
        }
        return liquibase;
    }
}
