package org.cherrypic.domain.tempalbum.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.dto.event.TempAlbumDeleteEvent;
import org.cherrypic.domain.tempalbum.dto.event.TempAlbumImagesDeleteEvent;
import org.cherrypic.s3.S3Util;
import org.cherrypic.s3.enums.ImageType;
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTempAlbumDeleteEvent(TempAlbumDeleteEvent event) {
        s3Util.deleteAllByImageTypeAndTargetId(ImageType.TEMP_ALBUM_IMAGE, event.tempAlbumId());
    }
}
