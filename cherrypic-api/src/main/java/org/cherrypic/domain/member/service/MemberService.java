package org.cherrypic.domain.member.service;

import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.NicknameUpdateRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.NicknameUpdateResponse;

public interface MemberService {
    MemberInfoResponse getMemberInfo();

    NicknameUpdateResponse updateNickname(NicknameUpdateRequest request);

    void saveFcmToken(FcmTokenSaveRequest request);
}
