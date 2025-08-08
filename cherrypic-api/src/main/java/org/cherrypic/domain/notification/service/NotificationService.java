package org.cherrypic.domain.notification.service;

import java.util.List;

public interface NotificationService {
    void sendAlbumDeleteNotification(Long albumId, Long senderId, List<Long> receiverIds);
}
