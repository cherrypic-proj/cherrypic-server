CREATE TABLE album_participation_history (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             member_id BIGINT NOT NULL,
                             album_title_snapshot VARCHAR(20) NOT NULL,
                             action VARCHAR(20) NOT NULL CHECK (action IN ('JOIN','LEAVE','KICK')),
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL
);
