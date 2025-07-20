package org.cherrypic.properties;

import org.cherrypic.aws.AwsProperties;
import org.cherrypic.jwt.JwtProperties;
import org.cherrypic.oidc.OidcProperties;
import org.cherrypic.redis.RedisProperties;
import org.cherrypic.s3.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    RedisProperties.class,
    OidcProperties.class,
    JwtProperties.class,
    AwsProperties.class,
    S3Properties.class
})
public class PropertiesConfig {}
