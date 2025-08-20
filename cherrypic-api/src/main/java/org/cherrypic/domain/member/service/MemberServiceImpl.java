package org.cherrypic.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.NicknameUpdateRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.NicknameUpdateResponse;
import org.cherrypic.domain.notification.service.FcmTokenService;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberUtil memberUtil;
    private final FcmTokenService fcmTokenService;

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse getMemberInfo() {
        final Member currentMember = memberUtil.getCurrentMember();
        return MemberInfoResponse.from(currentMember);
    }

    @Override
    public NicknameUpdateResponse updateNickname(NicknameUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        currentMember.updateNickname(request.nickname());

        return NicknameUpdateResponse.from(currentMember);
    }

    @Override
    @Transactional(readOnly = true)
    public void saveFcmToken(FcmTokenSaveRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        fcmTokenService.saveFcmToken(currentMember.getId(), request.fcmToken());
    }
}
