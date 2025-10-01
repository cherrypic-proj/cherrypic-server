package org.cherrypic.album.entity.tempalbum;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.cherrypic.exception.CustomException;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.cherrypic.tempalbum.exception.TempAlbumDomainErrorCode;
import org.junit.jupiter.api.Test;

class TempAlbumTest {

    @Test
    void 용량을_7자리_MB_이상으로_늘릴_경우_예외가_발생한다() {
        // given
        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        "testProfileImageUrl");
        TempAlbum tempAlbum = TempAlbum.createTempAlbum(member, "testTitle");

        // when & then
        assertThatThrownBy(() -> tempAlbum.increaseCapacity(BigDecimal.valueOf(12345678)))
                .isInstanceOf(CustomException.class)
                .hasMessage(
                        TempAlbumDomainErrorCode.TEMP_ALBUM_CAPACITY_INCREASE_OVER_LIMIT
                                .getMessage());
    }

    @Test
    void 용량을_0MB_미만으로_줄일_경우_예외가_발생한다() {
        // given
        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        "testProfileImageUrl");
        TempAlbum tempAlbum = TempAlbum.createTempAlbum(member, "testTitle");

        // when & then
        assertThatThrownBy(() -> tempAlbum.decreaseCapacity(BigDecimal.valueOf(1)))
                .isInstanceOf(CustomException.class)
                .hasMessage(
                        TempAlbumDomainErrorCode.TEMP_ALBUM_CAPACITY_DECREASE_UNDER_ZERO
                                .getMessage());
    }
}
