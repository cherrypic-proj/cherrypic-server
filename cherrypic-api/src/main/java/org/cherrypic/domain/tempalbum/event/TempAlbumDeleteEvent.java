package org.cherrypic.domain.tempalbum.event;

import org.cherrypic.tempalbum.entity.TempAlbum;

public record TempAlbumDeleteEvent(Long tempAlbumId) {
    public static TempAlbumDeleteEvent of(TempAlbum tempAlbum) {
        return new TempAlbumDeleteEvent(tempAlbum.getId());
    }
}
