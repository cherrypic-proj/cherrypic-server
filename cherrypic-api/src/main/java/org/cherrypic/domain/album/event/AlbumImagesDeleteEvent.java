package org.cherrypic.domain.album.event;

public record AlbumImagesDeleteEvent(Long albumId) {
    public static AlbumImagesDeleteEvent of(Long albumId) {
        return new AlbumImagesDeleteEvent(albumId);
    }
}
