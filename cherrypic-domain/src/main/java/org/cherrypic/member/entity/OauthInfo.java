package org.cherrypic.member.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthInfo {

    @NotNull private String oauthId;

    private String oauthProvider;
}
