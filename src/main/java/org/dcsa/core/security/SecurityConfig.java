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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Configures our application with Spring Security to restrict access to our API endpoints.
 */
@Slf4j
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Value("${dcsa.securityConfig.jwt.audience:localhost}")
    private String audience;

    @Value("${dcsa.securityConfig.jwt.claim.name:}")
    private String claimName;

    @Value("${dcsa.securityConfig.jwt.claim.value:}")
    private String claimValue;

    @Value("${dcsa.securityConfig.auth.enabled:false}")
    private boolean securityEnabled;

    @Value("${dcsa.securityConfig.csrf.enabled:false}")
    private boolean csrfEnabled;

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
            log.info("Security: auth enabled (dcsa.securityConfig.auth.enabled)");
            if (!receiveNotificationEndpoint.equals("NONE")) {
                endpoint = receiveNotificationEndpoint.replaceAll("/++$", "") + "/receive/*";
                securitySpec = securitySpec.pathMatchers(HttpMethod.POST, endpoint)
                        .permitAll()
                        .pathMatchers(HttpMethod.HEAD, endpoint)
                        .permitAll();

                log.info("Security: receive endpoint \"" + endpoint + "\"");
            } else {
                log.info("Security: No receive receive endpoint");
            }
            log.info("Security: JWT audience required: " + audience);
            if (!claimName.equals("") && !claimValue.equals("")) {
                String values = String.join(", ", claimValue);
                log.info("Security: JWT claims must have claim \"" + claimName + "\" containing one of: " + values);
                log.info("Security: JWT claims can be controlled via dcsa.securityConfig.jwt.claim.{name,value}");
            } else {
                log.info("Security: No claim requirements for JWT tokens (dcsa.securityConfig.jwt.claim.{name,value})");
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
            if (csrfEnabled) {
                log.info("Security: CSRF tokens required (dcsa.securityConfig.csrf.enabled)");
            } else {
                security.csrf().disable();
                log.info("Security: CSRF tokens disabled (dcsa.securityConfig.csrf.enabled)");
            }
        } else {
            log.info("Security: disabled - no authentication nor CSRF tokens needed (dcsa.securityConfig.{auth,csrf}.enabled)");
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
        OAuth2TokenValidator<Jwt> jwtValidator = ClaimsOneOfValueValidator.of(claimName, Set.of(claimValue));
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator,
                jwtValidator, new JwtTimestampValidator());

        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }
}
