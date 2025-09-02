package org.cherrypic.domain.album.event;

import org.cherrypic.domain.image.enums.BucketType;

public record AlbumImagesDeleteEvent(BucketType bucketType, Long albumId) {
    public static AlbumImagesDeleteEvent of(Long albumId) {
        return new AlbumImagesDeleteEvent(BucketType.MAIN, albumId);
    }
}
