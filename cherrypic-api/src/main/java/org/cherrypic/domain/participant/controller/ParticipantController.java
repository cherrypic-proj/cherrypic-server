package org.cherrypic.domain.participant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.domain.participant.service.ParticipantService;
import org.cherrypic.global.annotation.PageSize;
import org.cherrypic.global.pagination.SliceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums/{albumId}/participants")
@RequiredArgsConstructor
@Validated
@Tag(name = "6. 참가자 API", description = "앨범의 참가자 관련 API입니다.")
public class ParticipantController {

    private final ParticipantService participantService;

    @DeleteMapping("/me")
    @Operation(summary = "앨범 나가기", description = "앨범에서 나갑니다.")
    public ResponseEntity<Void> albumLeave(@PathVariable Long albumId) {
        participantService.leaveAlbum(albumId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{participantId}")
    @Operation(summary = "참가자 강퇴", description = "앨범 방장이 특정 참가자를 강퇴합니다.")
    public ResponseEntity<Void> participantKick(
            @PathVariable Long albumId, @PathVariable Long participantId) {
        participantService.kickParticipant(albumId, participantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "참가자 목록 조회", description = "특정 앨범의 참가자 목록을 커서 기반 페이징 방식으로 조회합니다.")
    public SliceResponse<ParticipantListResponse> participantsGet(
            @PathVariable Long albumId,
            @Parameter(description = "이전 페이지의 마지막 참가자 닉네임 (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    String lastNickname,
            @Parameter(description = "이전 페이지의 마지막 참가자 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastParticipantId,
            @Parameter(description = "페이지당 조회할 참가자 수") @RequestParam @PageSize Integer size) {
        return participantService.getParticipants(albumId, lastNickname, lastParticipantId, size);
    }
}
