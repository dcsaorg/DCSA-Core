package org.dcsa.core.security;


import lombok.RequiredArgsConstructor;
import org.dcsa.core.model.enums.ClaimShape;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;

/**
 * Validates that the JWT token contains the intended audience in its claims.
 */
@RequiredArgsConstructor(staticName = "of")
public class ClaimsOneOfValueValidator implements OAuth2TokenValidator<Jwt> {

    private final String claimName;
    private final Set<String> oneOf;
    private final ClaimShape claimShape;

    private OAuth2TokenValidatorResult missingClaim() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required claim " + claimName + " is missing", null));
    }

    private OAuth2TokenValidatorResult notMatchingExpectedClaimValue() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required claim " + claimName + " did not match one of the valid values", null));
    }

    private OAuth2TokenValidatorResult wrongClaimShape() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required claim " + claimName + " had the wrong shape/format (e.g. list vs. string)", null));
    }


    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (claimName.equals("") || oneOf.isEmpty()) {
            // No requirements, then we accept
            return OAuth2TokenValidatorResult.success();
        }
        if (!jwt.containsClaim(claimName)) {
            return missingClaim();
        }
        List<String> claimValues;
        try {
            switch (claimShape) {
                case STRING:
                    claimValues = List.of(jwt.getClaimAsString(claimName));
                    break;
                case LIST_OF_STRINGS:
                    claimValues = jwt.getClaimAsStringList(claimName);
                    break;
                default:
                    return wrongClaimShape();
            }
        } catch (IllegalArgumentException e) {
            // The jwt.getClaimAsX methods will throw IllegalArgumentException if the claim does not
            // match the "X" type implied.
            return wrongClaimShape();
        }
        for (String value : claimValues) {
            if (oneOf.contains(value)) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return notMatchingExpectedClaimValue();
    }
}
