package org.dcsa.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Configures our application with Spring Security to restrict access to our API endpoints.
 */
@Slf4j
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Value("${auth0.audience}")
    private String audience;

    @Value("${auth0.enabled}")
    private boolean securityEnabled;

    @Value("${dcsa.securityConfig.receiveNotificationEndpoint:NONE}")
    private String receiveNotificationEndpoint;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        /*
        This is where we configure the security required for our endpoints and setup our app to serve as
        an OAuth2 Resource Server, using JWT validation.
        */

        ServerHttpSecurity.AuthorizeExchangeSpec securitySpec = http.authorizeExchange();
        securitySpec.pathMatchers(HttpMethod.GET,"/actuator/health").permitAll();

        if (securityEnabled) {
            String endpoint = null;
            if (!receiveNotificationEndpoint.equals("NONE")) {
                endpoint = receiveNotificationEndpoint.replaceAll("/++$", "") + "/receive/*";
                securitySpec = securitySpec.pathMatchers(HttpMethod.POST, endpoint)
                        .permitAll()
                        .pathMatchers(HttpMethod.HEAD, endpoint)
                        .permitAll();

                log.info("Security: auth0 enabled - receive endpoint \"" + endpoint + "\"");
            } else {
                log.info("Security: auth0 enabled - no receive endpoint");
            }
            ServerHttpSecurity security = securitySpec.anyExchange().authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt()
                    .jwtAuthenticationConverter(new JwtAuthenticationConverter())
                    .and()
                    .and()
                    .cors()
                    .and();
            if (endpoint != null) {
                security.csrf().requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(
                        ServerWebExchangeMatchers.pathMatchers(endpoint)
                ));
            }

        } else {
            log.info("Security: disabled - no authentication nor CRSF tokens needed");
            securitySpec.anyExchange().permitAll()
            .and()
                    .csrf().disable();
        }
        return securitySpec
                .and()
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        /*
        By default, Spring Security does not validate the "aud" claim of the token, to ensure that this token is
        indeed intended for our app. Adding our own validator is easy to do:
        */

        NimbusReactiveJwtDecoder jwtDecoder = (NimbusReactiveJwtDecoder)
                ReactiveJwtDecoders.fromOidcIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator,
                new JwtTimestampValidator());

        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }
}
