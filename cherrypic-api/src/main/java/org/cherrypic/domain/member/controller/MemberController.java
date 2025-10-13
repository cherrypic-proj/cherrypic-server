package org.cherrypic.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.LocalImageDeletionToggleResponse;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.MemberProfileUpdateResponse;
import org.cherrypic.domain.member.dto.response.ParticipationHistoryResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.domain.member.service.ParticipationHistoryQueryService;
import org.cherrypic.global.annotation.PageSize;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "1-2. 회원 API", description = "회원 관련 API입니다.")
@Validated
public class MemberController {

    private final MemberService memberService;
    private final ParticipationHistoryQueryService participationHistoryQueryService;

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
    @Operation(summary = "로컬 이미지 삭제 허용 변경", description = "로컬 이미지 삭제 허용 여부를 변경합니다.")
    public LocalImageDeletionToggleResponse localImageDeletionToggle() {
        return memberService.toggleLocalImageDeletion();
    }

    @GetMapping("/me/participation-history")
    @Operation(summary = "회원의 앨범 참여 이력 조회", description = "사용자가 입장/퇴장/강퇴된 앨범 이력을 조회합니다.")
    public SliceResponse<ParticipationHistoryResponse> participationHistoryGet(
            @Parameter(description = "이전 페이지의 마지막 앨범 참여 이력 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastHistoryId,
            @Parameter(description = "페이지당 조회할 참여 이력 수") @RequestParam @PageSize Integer size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return participationHistoryQueryService.getParticipationHistory(
                lastHistoryId, size, direction);
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
