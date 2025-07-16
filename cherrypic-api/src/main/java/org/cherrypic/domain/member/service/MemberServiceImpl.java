package org.cherrypic.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse getMemberInfo() {
        final Member currentMember = memberUtil.getCurrentMember();

        return MemberInfoResponse.from(currentMember);
    }
}
