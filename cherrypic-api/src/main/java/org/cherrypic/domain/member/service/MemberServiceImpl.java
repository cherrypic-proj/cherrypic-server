package org.cherrypic.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.event.ImageDeleteEvent;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.MemberProfileUpdateResponse;
import org.cherrypic.domain.notification.service.FcmTokenService;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberUtil memberUtil;
    private final FcmTokenService fcmTokenService;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse getMemberInfo() {
        final Member currentMember = memberUtil.getCurrentMember();
        return MemberInfoResponse.from(currentMember);
    }

    @Override
    public MemberProfileUpdateResponse updateProfile(MemberProfileUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        if (currentMember.getProfileImageUrl() != null
                && !currentMember.getProfileImageUrl().equals(request.profileImageUrl())) {
            eventPublisher.publishEvent(ImageDeleteEvent.of(currentMember.getProfileImageUrl()));
        }

        currentMember.updateMember(request.nickname(), request.profileImageUrl());

        return MemberProfileUpdateResponse.from(currentMember);
    }

    @Override
    @Transactional(readOnly = true)
    public void saveFcmToken(FcmTokenSaveRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        fcmTokenService.saveFcmToken(currentMember.getId(), request.fcmToken());
    }
}
