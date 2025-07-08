package org.cherrypic.oidc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oidc")
public record OidcProperties(KAKAO kakao, Apple apple) {
    public record KAKAO(String jwkSetUri, String issuer, String audience) {}

    public record Apple(String jwkSetUri, String issuer, String audience) {}
}
