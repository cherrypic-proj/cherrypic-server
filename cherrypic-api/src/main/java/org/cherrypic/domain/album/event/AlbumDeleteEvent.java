package org.cherrypic.domain.album.event;

import java.util.List;

public record AlbumDeleteEvent(
        Long albumId,
        Long senderId,
        String hostNickname,
        String albumTitle,
        List<Long> receiverIds) {
    public static AlbumDeleteEvent of(
            Long albumId,
            Long senderId,
            String hostNickname,
            String albumTitle,
            List<Long> receiverIds) {
        return new AlbumDeleteEvent(albumId, senderId, hostNickname, albumTitle, receiverIds);
    }
}
