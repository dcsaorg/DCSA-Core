package org.dcsa.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import java.util.function.Function;

@Configuration
@Slf4j
public class DCSACoreConfiguration {

    @Value("${dcsa.supportProxyHeaders:true}")
    private boolean supportProxyHeaders;

    @Bean
    public R2dbcDialect r2dbcDialectBean(DatabaseClient databaseClient) {
        return DialectResolver.getDialect(databaseClient.getConnectionFactory());
    }

    @Bean
    public Function<ServerHttpRequest, ServerHttpRequest> forwardedHeaderTransformer() {
        if (!supportProxyHeaders) {
            log.info("Disabled support for Proxy headers (X-Forwarded-*, etc.). Use dcsa.supportProxyHeader=[true|false] to change this");
            return Function.identity();
        }
        log.info("Enabled support for Proxy headers (X-Forwarded-*, etc.) from *any* IP. Use dcsa.supportProxyHeader=[true|false] to change this");
        return new ForwardedHeaderTransformer();
    }
}
