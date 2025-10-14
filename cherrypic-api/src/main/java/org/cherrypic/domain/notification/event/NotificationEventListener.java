package org.cherrypic.domain.notification.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.dto.event.AlbumDeleteNotificationSendEvent;
import org.cherrypic.domain.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAlbumDeleteNotificationSendEvent(AlbumDeleteNotificationSendEvent event) {
        notificationService.sendAlbumDeleteNotification(
                event.albumId(),
                event.senderId(),
                event.hostNickname(),
                event.albumTitle(),
                event.receiverIds());
    }
}
