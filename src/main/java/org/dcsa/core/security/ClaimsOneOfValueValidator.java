package org.dcsa.core.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

/**
 * Validates that the JWT token contains the intended audience in its claims.
 */
@RequiredArgsConstructor(staticName = "of")
public class ClaimsOneOfValueValidator implements OAuth2TokenValidator<Jwt> {

    private final String claimName;
    private final Set<String> oneOf;

    private OAuth2TokenValidatorResult missingClaim() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required claim " + claimName + " is missing", null));
    }

    private OAuth2TokenValidatorResult notMatchingExpectedClaimValue() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required claim " + claimName + " did not match one of the valid values", null));
    }


    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (claimName.equals("") || oneOf.isEmpty()) {
            // No requirements, then we accept
            return OAuth2TokenValidatorResult.success();
        }
        String claimValue = jwt.getClaimAsString(claimName);
        if (claimValue == null) {
            return missingClaim();
        }
        if (oneOf.contains(claimValue)) {
            return OAuth2TokenValidatorResult.success();
        }
        return notMatchingExpectedClaimValue();
    }
}
