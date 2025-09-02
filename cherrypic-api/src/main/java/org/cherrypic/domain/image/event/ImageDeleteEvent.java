package org.cherrypic.domain.image.event;

import org.cherrypic.domain.image.enums.BucketType;

public record ImageDeleteEvent(BucketType bucketType, String imageUrl) {
    public static ImageDeleteEvent of(BucketType bucketType, String imageUrl) {
        return new ImageDeleteEvent(bucketType, imageUrl);
    }
}
