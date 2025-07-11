package org.cherrypic.domain.auth.service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.auth.enums.OauthProvider;
import org.cherrypic.domain.auth.exception.AuthErrorCode;
import org.cherrypic.domain.auth.exception.AuthException;
import org.cherrypic.oidc.OidcProperties;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdTokenVerifier {

    private final OidcProperties oidcProperties;
    private Map<OauthProvider, JwtDecoder> decoders;

    @PostConstruct
    private void initDecoders() {
        this.decoders =
                Map.of(
                        OauthProvider.KAKAO, buildDecoder(oidcProperties.kakao().jwkSetUri()),
                        OauthProvider.APPLE, buildDecoder(oidcProperties.apple().jwkSetUri()));
    }

    private JwtDecoder buildDecoder(String jwkSetUrl) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUrl).build();
    }

    public OidcUser getOidcUser(String idToken, OauthProvider provider) {
        Jwt jwt = getJwt(idToken, provider);
        OidcIdToken oidcIdToken = getOidcIdToken(jwt);

        String audience =
                switch (provider) {
                    case KAKAO -> oidcProperties.kakao().audience();
                    case APPLE -> oidcProperties.apple().audience();
                };

        String issuer =
                switch (provider) {
                    case KAKAO -> oidcProperties.kakao().issuer();
                    case APPLE -> oidcProperties.apple().issuer();
                };

        validateAudience(oidcIdToken, audience);
        validateIssuer(oidcIdToken, issuer);
        validateExpiresAt(oidcIdToken);

        return new DefaultOidcUser(null, oidcIdToken);
    }

    private Jwt getJwt(String idToken, OauthProvider provider) {
        return decoders.get(provider).decode(idToken);
    }

    private OidcIdToken getOidcIdToken(Jwt jwt) {
        return new OidcIdToken(
                jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
    }

    private void validateAudience(OidcIdToken oidcIdToken, String targetAudience) {
        String audience = oidcIdToken.getAudience().getFirst();

        if (!targetAudience.equals(audience)) {
            throw new AuthException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED);
        }
    }

    private void validateIssuer(OidcIdToken oidcIdToken, String targetIssuer) {
        String issuer = oidcIdToken.getIssuer().toString();

        if (!targetIssuer.equals(issuer)) {
            throw new AuthException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED);
        }
    }

    private void validateExpiresAt(OidcIdToken oidcIdToken) {
        Instant expiresAt = oidcIdToken.getExpiresAt();

        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new AuthException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED);
        }
    }
}
