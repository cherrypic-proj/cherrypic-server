package org.cherrypic.domain.notification.repository;

import java.util.List;
import org.cherrypic.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
            Long albumId, Long senderId, String title, String content);

    @Modifying
    @Query("delete from Notification n where n.receiver.id = :receiverId and n.album.id = :albumId")
    void deleteByReceiverIdAndAlbumId(Long receiverId, Long albumId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Notification n where n.album.id = :albumId")
    void deleteAllByAlbumId(Long albumId);

    @Query("select n from Notification n where n.album.id = :albumId")
    List<Notification> findAllByAlbumId(Long albumId);
}
