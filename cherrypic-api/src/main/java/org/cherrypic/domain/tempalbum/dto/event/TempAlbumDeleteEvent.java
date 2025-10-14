package org.cherrypic.domain.tempalbum.dto.event;

import org.cherrypic.tempalbum.entity.TempAlbum;

public record TempAlbumDeleteEvent(Long tempAlbumId) {
    public static TempAlbumDeleteEvent of(TempAlbum tempAlbum) {
        return new TempAlbumDeleteEvent(tempAlbum.getId());
    }
}
