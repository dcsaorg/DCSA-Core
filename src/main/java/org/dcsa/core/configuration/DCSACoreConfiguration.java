package org.dcsa.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.r2dbc.core.DatabaseClient;

@Configuration
public class DCSACoreConfiguration {
    @Bean
    public R2dbcDialect r2dbcDialectBean(DatabaseClient databaseClient) {
        return DialectResolver.getDialect(databaseClient.getConnectionFactory());
    }
}
