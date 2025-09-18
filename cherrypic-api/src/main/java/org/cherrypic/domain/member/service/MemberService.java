package org.cherrypic.domain.member.service;

import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.MarketingAgreeToggleResponse;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.MemberProfileUpdateResponse;
import org.cherrypic.domain.member.dto.response.ServiceAlarmAgreeToggleResponse;

public interface MemberService {
    MemberInfoResponse getMemberInfo();

    MemberProfileUpdateResponse updateProfile(MemberProfileUpdateRequest request);

    void saveFcmToken(FcmTokenSaveRequest request);

    ServiceAlarmAgreeToggleResponse toggleServiceAlarmAgree();

    MarketingAgreeToggleResponse toggleMarketingAgree();
}
