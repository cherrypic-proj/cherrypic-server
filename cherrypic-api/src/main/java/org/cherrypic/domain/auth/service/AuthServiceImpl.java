package org.cherrypic.domain.auth.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.auth.dto.AccessTokenDto;
import org.cherrypic.domain.auth.dto.RefreshTokenDto;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.dto.response.TokenReissueResponse;
import org.cherrypic.domain.auth.enums.OauthProvider;
import org.cherrypic.domain.auth.exception.AuthErrorCode;
import org.cherrypic.domain.auth.exception.AuthException;
import org.cherrypic.domain.auth.repository.RefreshTokenRepository;
import org.cherrypic.domain.auth.util.NicknameGenerator;
import org.cherrypic.domain.member.exception.MemberErrorCode;
import org.cherrypic.domain.member.exception.MemberException;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final MemberUtil memberUtil;
    private final MemberRepository memberRepository;

    private final RefreshTokenRepository refreshTokenRepository;
    private final IdTokenVerifier idTokenVerifier;
    private final JwtTokenService jwtTokenService;
    private final NicknameGenerator nicknameGenerator;

    @Override
    public SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request) {
        OidcUser oidcUser = idTokenVerifier.getOidcUser(request.idToken(), provider);

        Optional<Member> optionalMember = findByOidcUser(oidcUser);
        Member member = optionalMember.orElseGet(() -> saveMember(oidcUser));

        return getLoginResponse(member);
    }

    @Override
    public TokenReissueResponse reissueToken(String refreshTokenValue) {
        RefreshTokenDto oldRefreshTokenDto =
                jwtTokenService.retrieveRefreshToken(refreshTokenValue);

        if (oldRefreshTokenDto == null) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenDto newRefreshTokenDto =
                jwtTokenService.reissueRefreshToken(oldRefreshTokenDto);
        AccessTokenDto newAccessTokenDto =
                jwtTokenService.reissueAccessToken(getMember(newRefreshTokenDto));

        return TokenReissueResponse.of(
                newAccessTokenDto.tokenValue(), newRefreshTokenDto.tokenValue());
    }

    @Override
    public void logoutMember() {
        final Member currentMember = memberUtil.getCurrentMember();

        refreshTokenRepository
                .findById(currentMember.getId())
                .ifPresent(refreshTokenRepository::delete);
    }

    private SocialLoginResponse getLoginResponse(Member member) {
        String accessToken = jwtTokenService.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenService.createRefreshToken(member.getId());
        return SocialLoginResponse.of(accessToken, refreshToken);
    }

    private Member saveMember(OidcUser oidcUser) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);

        Member member =
                Member.createMember(
                        oauthInfo, nicknameGenerator.generateNickname(), oidcUser.getPicture());
        return memberRepository.save(member);
    }

    private Optional<Member> findByOidcUser(OidcUser oidcUser) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);
        return memberRepository.findByOauthInfo(oauthInfo);
    }

    private OauthInfo extractOauthInfo(OidcUser oidcUser) {
        return OauthInfo.createOauthInfo(oidcUser.getSubject(), oidcUser.getIssuer().toString());
    }

    private Member getMember(RefreshTokenDto refreshTokenDto) {
        return memberRepository
                .findById(refreshTokenDto.memberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
