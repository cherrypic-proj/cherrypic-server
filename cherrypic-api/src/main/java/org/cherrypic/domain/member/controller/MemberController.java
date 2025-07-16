package org.cherrypic.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "1-2. 회원 API", description = "회원 관련 API입니다.")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(summary = "회원 정보 조회", description = "로그인한 회원 정보를 조회합니다.")
    public MemberInfoResponse memberInfo() {
        return memberService.getMemberInfo();
    }
}
