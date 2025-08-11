package org.cherrypic.domain.member.service;

import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;

public interface MemberService {
    MemberInfoResponse getMemberInfo();

    void saveFcmToken(FcmTokenSaveRequest request);
}
