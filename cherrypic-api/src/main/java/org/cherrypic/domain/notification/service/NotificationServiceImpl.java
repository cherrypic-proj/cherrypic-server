package org.cherrypic.domain.notification.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String PUSH_ALBUM_DELETE_TITLE = "앨범 삭제 안내";
    private static final String PUSH_ALBUM_DELETE_BODY =
            "%s님이 %s 앨범을 삭제하려고 합니다. 중요한 사진은 미리 다운로드받은 뒤 앨범에서 나가주세요.";

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void sendAlbumDeleteNotification(
            Long albumId,
            Long senderId,
            String hostNickname,
            String albumTitle,
            List<Long> receiverIds) {
        final String content = String.format(PUSH_ALBUM_DELETE_BODY, hostNickname, albumTitle);

        notificationRepository.bulkInsertAlbumDeleteNotifications(
                albumId, senderId, PUSH_ALBUM_DELETE_TITLE, content);

        List<Long> serviceAlarmAgreedIds = memberRepository.findServiceAlarmAgreedIds(receiverIds);

        List<String> tokens = fcmTokenService.getFcmTokens(serviceAlarmAgreedIds);
        if (tokens.isEmpty()) {
            return;
        }

        fcmService.sendGroupMessageAsync(tokens, PUSH_ALBUM_DELETE_TITLE, content);
    }
}
