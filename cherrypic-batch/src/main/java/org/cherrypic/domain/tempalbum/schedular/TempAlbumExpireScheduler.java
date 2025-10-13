package org.cherrypic.domain.tempalbum.schedular;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.job.TempAlbumExpireJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TempAlbumExpireScheduler {

    private final TempAlbumExpireJob tempAlbumExpireJob;

    @Scheduled(cron = "0 0 0 * * *")
    public void runTempAlbumExpirejob() {
        tempAlbumExpireJob.run();
    }
}
