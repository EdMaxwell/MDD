package com.mdd.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the Liquibase bean explicitly so dev contexts can be configured from properties.
 */
@Configuration
public class LiquibaseConfig {

    /**
     * Builds the Liquibase runner from Spring configuration properties.
     *
     * @param dataSource application datasource
     * @param changeLog changelog location
     * @param contexts optional Liquibase contexts
     * @param enabled whether migrations should run
     * @return configured Liquibase bean
     */
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
