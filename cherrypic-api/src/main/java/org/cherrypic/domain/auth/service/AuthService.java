package org.cherrypic.domain.auth.service;

import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.entity.OauthProvider;

public interface AuthService {
    SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request);
}
