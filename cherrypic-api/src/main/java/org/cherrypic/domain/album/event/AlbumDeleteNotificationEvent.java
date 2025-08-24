package org.cherrypic.domain.album.event;

import java.util.List;

public record AlbumDeleteNotificationEvent(
        Long albumId,
        Long senderId,
        String hostNickname,
        String albumTitle,
        List<Long> receiverIds) {
    public static AlbumDeleteNotificationEvent of(
            Long albumId,
            Long senderId,
            String hostNickname,
            String albumTitle,
            List<Long> receiverIds) {
        return new AlbumDeleteNotificationEvent(
                albumId, senderId, hostNickname, albumTitle, receiverIds);
    }
}
