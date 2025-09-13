package org.cherrypic.domain.tempalbum.event;

import java.util.List;

public record TempAlbumImagesDeleteEvent(List<String> tempImageUrls) {
    public static TempAlbumImagesDeleteEvent of(List<String> tempImageUrls) {
        return new TempAlbumImagesDeleteEvent(tempImageUrls);
    }
}
