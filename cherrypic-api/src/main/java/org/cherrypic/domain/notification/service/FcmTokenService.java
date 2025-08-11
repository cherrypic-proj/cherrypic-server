package org.cherrypic.domain.notification.service;

import java.util.*;
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

    public List<String> getFcmTokens(List<Long> memberIds) {
        Set<String> tokens = new HashSet<>();
        for (Long id : memberIds) {
            Set<String> s = redisTemplate.opsForSet().members(String.format(FCM_TOKEN_KEY, id));
            if (s != null) {
                tokens.addAll(s);
            }
        }
        return new ArrayList<>(tokens);
    }
}
