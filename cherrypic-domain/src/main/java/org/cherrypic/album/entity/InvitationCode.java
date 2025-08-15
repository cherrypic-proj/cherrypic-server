package org.cherrypic.album.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@RedisHash(value = "invitationCode")
public class InvitationCode {

    @Id private Long albumId;

    private String code;

    @TimeToLive private long ttl;

    @Builder
    private InvitationCode(Long albumId, String code, long ttl) {
        this.albumId = albumId;
        this.code = code;
        this.ttl = ttl;
    }
}
