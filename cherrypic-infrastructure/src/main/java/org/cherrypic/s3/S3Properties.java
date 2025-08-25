package org.cherrypic.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("aws.s3")
public record S3Properties(String mainBucket, String tempAlbumBucket, String endpoint) {}
