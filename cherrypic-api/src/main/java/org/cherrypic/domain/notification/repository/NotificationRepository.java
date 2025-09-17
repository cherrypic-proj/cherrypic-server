package org.cherrypic.domain.notification.repository;

import org.cherrypic.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    """
            insert into notification (sender_id, receiver_id, album_id, title, content, type, created_at, updated_at)
            select :senderId, p.member_id, :albumId, :title, :content, 'ALBUM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
            from participant p
            where p.album_id = :albumId
              and p.member_id <> :senderId
            """,
            nativeQuery = true)
    void bulkInsertAlbumDeleteNotifications(
            @Param("albumId") Long albumId,
            @Param("senderId") Long senderId,
            @Param("title") String title,
            @Param("content") String content);

    @Modifying
    @Query("delete from Notification n where n.receiver.id = :receiverId and n.album.id = :albumId")
    void deleteByReceiverIdAndAlbumId(
            @Param("receiverId") Long receiverId, @Param("albumId") Long albumId);
}
