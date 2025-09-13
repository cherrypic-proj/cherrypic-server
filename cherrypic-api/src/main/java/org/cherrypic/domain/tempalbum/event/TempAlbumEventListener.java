package org.cherrypic.domain.tempalbum.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.s3.S3Util;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TempAlbumEventListener {

    private final S3Util s3Util;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTempAlbumImagesDeleteEvent(TempAlbumImagesDeleteEvent event) {
        s3Util.deleteAllByUrls(event.tempImageUrls());
    }
}
