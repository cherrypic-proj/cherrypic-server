package org.cherrypic.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.LocalImageDeletionToggleResponse;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.MemberProfileUpdateResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.springframework.http.ResponseEntity;
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

    @PatchMapping("/me")
    @Operation(summary = "회원 프로필 수정", description = "회원의 닉네임 및 프로필 이미지를 수정합니다.")
    public MemberProfileUpdateResponse memberProfileUpdate(
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        return memberService.updateProfile(request);
    }

    @PatchMapping("/me/local-image-deletion")
    @Operation(summary = "로컬 이미지 삭제 토글 상태 변경", description = "로컬 이미지 삭제 토글 상태를 변경합니다.")
    public LocalImageDeletionToggleResponse localImageDeletionToggle() {
        return memberService.toggleLocalImageDeletion();
    }

    @PostMapping("/fcm-tokens")
    @Operation(
            summary = "FCM 토큰 저장",
            description = "클라이언트에서 발급된 FCM 토큰을 서버에 저장합니다. 한 계정에 여러 디바이스(다중 토큰)를 지원합니다.")
    public ResponseEntity<Void> memberFcmTokenSave(
            @Valid @RequestBody FcmTokenSaveRequest request) {
        memberService.saveFcmToken(request);
        return ResponseEntity.noContent().build();
    }
}
