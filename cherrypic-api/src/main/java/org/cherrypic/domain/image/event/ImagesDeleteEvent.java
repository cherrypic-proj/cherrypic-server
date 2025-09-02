package org.cherrypic.domain.image.event;

import java.util.List;
import org.cherrypic.domain.image.enums.BucketType;

public record ImagesDeleteEvent(BucketType bucketType, List<String> imageUrls) {
    public static ImagesDeleteEvent of(BucketType bucketType, List<String> imageUrls) {
        return new ImagesDeleteEvent(bucketType, imageUrls);
    }
}
