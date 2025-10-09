package org.cherrypic.domain.tempalbum.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumUpdateRequest;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumCreateResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumInfoResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumListResponse;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TempAlbumServiceImpl implements TempAlbumService {

    private final MemberUtil memberUtil;

    private final TempAlbumRepository tempAlbumRepository;

    @Override
    @Transactional
    public TempAlbumCreateResponse createTempAlbum(TempAlbumCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateTempAlbumCreateLimit(currentMember);

        TempAlbum tempAlbum = TempAlbum.createTempAlbum(currentMember, request.title());
        tempAlbumRepository.save(tempAlbum);

        return TempAlbumCreateResponse.from(tempAlbum);
    }

    @Override
    public TempAlbumListResponse getTempAlbums() {
        final Member currentMember = memberUtil.getCurrentMember();
        final List<TempAlbum> tempAlbums =
                tempAlbumRepository.findAllByMemberIdOrderByIdDesc(currentMember.getId());

        return TempAlbumListResponse.from(tempAlbums);
    }

    @Override
    @Transactional
    public void updateTempAlbum(Long tempAlbumId, TempAlbumUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final TempAlbum tempAlbum = getTempAlbumById(tempAlbumId);

        validateTempAlbumOwner(tempAlbum, currentMember.getId());

        tempAlbum.updateTempAlbum(request.title(), request.webUrl());
    }

    @Override
    public TempAlbumInfoResponse getTempAlbum(Long tempAlbumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final TempAlbum tempAlbum = getTempAlbumById(tempAlbumId);

        validateTempAlbumOwner(tempAlbum, currentMember.getId());

        return TempAlbumInfoResponse.of(
                tempAlbum.getTitle(),
                tempAlbum.getCapacityMb(),
                tempAlbum.getType().getCapacityMb(),
                tempAlbum.getExpiredAt(),
                tempAlbum.getWebUrl());
    }

    private void validateTempAlbumCreateLimit(Member member) {
        long count = tempAlbumRepository.countByMemberId(member.getId());
        if (count >= 5) {
            throw new CustomException(TempAlbumErrorCode.CREATE_OVER_LIMIT);
        }
    }

    private void validateTempAlbumOwner(TempAlbum tempAlbum, Long memberId) {
        if (!Objects.equals(tempAlbum.getId(), memberId)) {
            throw new CustomException(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER);
        }
    }

    private TempAlbum getTempAlbumById(Long tempAlbumId) {
        return tempAlbumRepository
                .findById(tempAlbumId)
                .orElseThrow(() -> new CustomException(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND));
    }
}
