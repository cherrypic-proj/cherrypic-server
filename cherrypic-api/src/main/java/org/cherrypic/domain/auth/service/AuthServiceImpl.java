package org.cherrypic.domain.auth.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.entity.OauthProvider;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.repository.MemberRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final IdTokenVerifier idTokenVerifier;
    private final JwtTokenService jwtTokenService;

    @Override
    public SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request) {
        OidcUser oidcUser = idTokenVerifier.getOidcUser(request.idToken(), provider);

        Optional<Member> optionalMember = findByOidcUser(oidcUser);
        Member member = optionalMember.orElseGet(() -> saveMember(oidcUser));

        return getLoginResponse(member);
    }

    private SocialLoginResponse getLoginResponse(Member member) {
        String accessToken = jwtTokenService.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenService.createRefreshToken(member.getId());
        return SocialLoginResponse.of(accessToken, refreshToken);
    }

    private Member saveMember(OidcUser oidcUser) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);

        Member member =
                Member.createMember(oauthInfo, oidcUser.getNickName(), oidcUser.getPicture());
        return memberRepository.save(member);
    }

    private Optional<Member> findByOidcUser(OidcUser oidcUser) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);
        return memberRepository.findByOauthInfo(oauthInfo);
    }

    private OauthInfo extractOauthInfo(OidcUser oidcUser) {
        return OauthInfo.createOauthInfo(oidcUser.getSubject(), oidcUser.getIssuer().toString());
    }
}
