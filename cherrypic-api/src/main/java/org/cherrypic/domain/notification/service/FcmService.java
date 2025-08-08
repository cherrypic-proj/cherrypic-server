package org.cherrypic.domain.notification.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public ApiFuture<BatchResponse> sendGroupMessageAsync(
            List<String> tokens, String title, String content) {
        MulticastMessage multicast =
                MulticastMessage.builder()
                        .addAllTokens(tokens)
                        .setNotification(
                                Notification.builder().setTitle(title).setBody(content).build())
                        .build();

        return FirebaseMessaging.getInstance().sendEachForMulticastAsync(multicast);
    }

    public ApiFuture<String> sendMessageAsync(String token, String title, String content) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        Message message =
                Message.builder()
                        .setToken(token)
                        .setNotification(
                                Notification.builder().setTitle(title).setBody(content).build())
                        .build();

        return FirebaseMessaging.getInstance().sendAsync(message);
    }
}
