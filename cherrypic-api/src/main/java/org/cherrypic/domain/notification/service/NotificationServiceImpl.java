package org.cherrypic.domain.notification.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.repository.AlbumRepository;
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
            "%s님이 '%s' 앨범을 삭제하려고 합니다. 이미지를 백업한 후 앨범에서 나가주세요.";

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;

    private final MemberRepository memberRepository;
    private final AlbumRepository albumRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void sendAlbumDeleteNotification(Long albumId, Long senderId, List<Long> receiverIds) {
        final String hostNickname = getMemberNicknameById(senderId);
        final String albumTitle = getAlbumTitleById(albumId);

        notificationRepository.bulkInsertAlbumDeleteNotifications(albumId, senderId);

        List<String> tokens = fcmTokenService.getFcmTokens(receiverIds);
        if (tokens.isEmpty()) {
            return;
        }

        fcmService.sendGroupMessageAsync(
                tokens,
                PUSH_ALBUM_DELETE_TITLE,
                String.format(PUSH_ALBUM_DELETE_BODY, hostNickname, albumTitle));
    }

    private String getMemberNicknameById(Long senderId) {
        return memberRepository.findNicknameById(senderId);
    }

    private String getAlbumTitleById(Long albumId) {
        return albumRepository.findTitleById(albumId);
    }
}
