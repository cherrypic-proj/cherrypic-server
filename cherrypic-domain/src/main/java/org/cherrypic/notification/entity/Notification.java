package org.cherrypic.notification.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.member.entity.Member;
import org.cherrypic.notification.enums.NotificationType;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Member sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @NotNull private String title;

    @NotNull private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(
            Member sender,
            Member receiver,
            Album album,
            String title,
            String content,
            NotificationType type) {
        this.sender = sender;
        this.receiver = receiver;
        this.album = album;
        this.title = title;
        this.content = content;
        this.type = type;
    }

    public static Notification createNotification(
            Member sender,
            Member receiver,
            Album album,
            String title,
            String content,
            NotificationType type) {
        return Notification.builder()
                .sender(sender)
                .receiver(receiver)
                .album(album)
                .title(title)
                .content(content)
                .type(type)
                .build();
    }
}
