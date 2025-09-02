package org.cherrypic.domain.tempalbum.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateResponse;
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

    private void validateTempAlbumCreateLimit(Member member) {
        long count = tempAlbumRepository.countByMemberId(member.getId());
        if (count >= 5) {
            throw new CustomException(TempAlbumErrorCode.CREATE_OVER_LIMIT);
        }
    }
}
