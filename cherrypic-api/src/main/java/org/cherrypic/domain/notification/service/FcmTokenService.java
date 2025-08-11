package org.cherrypic.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private static final String FCM_TOKEN_KEY = "fcmToken:%d";

    private final StringRedisTemplate redisTemplate;

    public void saveFcmToken(Long memberId, String token) {
        redisTemplate.opsForSet().add(String.format(FCM_TOKEN_KEY, memberId), token);
    }
}
