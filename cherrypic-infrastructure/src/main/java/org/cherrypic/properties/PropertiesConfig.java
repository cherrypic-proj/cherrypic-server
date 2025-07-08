package org.cherrypic.properties;

import org.cherrypic.jwt.JwtProperties;
import org.cherrypic.oidc.OidcProperties;
import org.cherrypic.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RedisProperties.class, OidcProperties.class, JwtProperties.class})
public class PropertiesConfig {}
