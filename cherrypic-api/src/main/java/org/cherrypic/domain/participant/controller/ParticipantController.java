package org.cherrypic.domain.participant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.participant.service.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/albums/{albumId}/participants")
@RequiredArgsConstructor
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
}
