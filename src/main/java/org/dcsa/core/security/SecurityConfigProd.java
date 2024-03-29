package org.dcsa.core.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcsa.core.model.enums.ClaimShape;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
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

import java.util.Set;

/** Configures our application with Spring Security to restrict access to our API endpoints. */
@Profile("prod")
@Slf4j
@EnableWebFluxSecurity
public class SecurityConfigProd {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  private String issuer;

  @Value("${dcsa.securityConfig.jwt.audience:NONE}")
  private String audience;

  @Value("${dcsa.securityConfig.jwt.claim.name:}")
  private String claimName;

  @Value("${dcsa.securityConfig.jwt.claim.value:}")
  private String claimValue;

  @Value("${dcsa.securityConfig.jwt.claim.shape:STRING}")
  private ClaimShape claimShape;

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

    String endpoint = null;
    log.info("Security: auth enabled (dcsa.securityConfig.auth.enabled)");
    log.info("Security: JWT with Issuer URI: " + issuer);
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
    log.info("Security: JWT issuer-uri: {}", issuer);
    log.info("Security: JWT audience required: " + audience);
    if (!claimName.equals("") && !claimValue.equals("")) {
      String values = String.join(", ", claimValue);
      log.info("Security: JWT claims must have claim \"{}\" (shape: {}) containing one of: {}",
        claimName, claimShape, values);
      log.info("Security: JWT claims can be controlled via dcsa.securityConfig.jwt.claim.{name,value,shape}");
    } else {
      log.info("Security: No claim requirements for JWT tokens (dcsa.securityConfig.jwt.claim.{name,value,shape})");
    }

    ServerHttpSecurity security = securitySpec.anyExchange().authenticated()
      .and();

    if(StringUtils.isNotEmpty(issuer)){
      security.oauth2ResourceServer().jwt()
        .jwtAuthenticationConverter(new JwtAuthenticationConverter()).and();
    }

    security.cors();

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

    return securitySpec
      .and()
      .build();
  }

  @Bean
  @ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isNotEmpty('${spring.security.oauth2.resourceserver.jwt.issuer-uri:}')")
  ReactiveJwtDecoder jwtDecoder() {
        /*
        By default, Spring Security does not validate the "aud" claim of the token, to ensure that this token is
        indeed intended for our app. Adding our own validator is easy to do:
        */

    NimbusReactiveJwtDecoder jwtDecoder = (NimbusReactiveJwtDecoder)
      ReactiveJwtDecoders.fromOidcIssuerLocation(issuer);

    OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
    OAuth2TokenValidator<Jwt> jwtValidator = ClaimsOneOfValueValidator.of(claimName, Set.of(claimValue), claimShape);
    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
    OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator,
      jwtValidator, new JwtTimestampValidator());

    jwtDecoder.setJwtValidator(withAudience);
    return jwtDecoder;
  }

  @EventListener(ApplicationStartedEvent.class)
  @ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isEmpty('${spring.security.oauth2.resourceserver.jwt.issuer-uri:}')")
  void securityLogConfiguration() {
    log.info(
      "JWT is disabled as `spring.security.oauth2.resourceserver.jwt.issuer-uri` is missing.");
  }
}
