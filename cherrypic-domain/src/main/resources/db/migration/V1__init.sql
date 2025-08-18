CREATE TABLE member (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        nickname VARCHAR(50) NOT NULL,
                        oauth_id VARCHAR(255) NOT NULL,
                        oauth_provider VARCHAR(255) NOT NULL,
                        profile_image_url VARCHAR(255),
                        role VARCHAR(255)  NOT NULL CHECK(role IN ('ADMIN','USER')),
                        status VARCHAR(255) NOT NULL CHECK(status IN ('NORMAL','DELETED','FORBIDDEN')),
                        app_alarm BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL
);


CREATE TABLE album (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       title VARCHAR(20) NOT NULL,
                       cover_url VARCHAR(255),
                       plan VARCHAR(255) CHECK (plan IN ('BASIC','PRO','PREMIUM')),
                       permission_control BOOLEAN NOT NULL,
                       capacity_gb DECIMAL(6,2) NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL
);


CREATE TABLE payment (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         member_id BIGINT NOT NULL,
                         album_id BIGINT,
                         merchant_uid VARCHAR(255) NOT NULL,
                         imp_uid VARCHAR(255),
                         pg_provider VARCHAR(255),
                         amount INT NOT NULL,
                         status VARCHAR(255) NOT NULL CHECK (status IN ('READY','PAID','FAILED','CANCELLED')),
                         paid_at DATETIME,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES member (id),
                         CONSTRAINT fk_payment_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE subscription (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              member_id BIGINT NOT NULL,
                              album_id BIGINT UNIQUE,
                              status VARCHAR(255) NOT NULL CHECK (status IN ('ACTIVE','CANCELLED','EXPIRED')),
                              start_at DATETIME(6),
                              end_at DATETIME(6),
                              next_billing_at DATETIME(6),
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6) NOT NULL,
                              CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES member (id),
                              CONSTRAINT fk_subscription_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE event (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       album_id BIGINT NOT NULL,
                       title VARCHAR(20) NOT NULL,
                       cover_url VARCHAR(255),
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       CONSTRAINT fk_event_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE image (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       album_id BIGINT NOT NULL,
                       url VARCHAR(255) NOT NULL,
                       generated_at DATETIME,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       CONSTRAINT fk_image_album FOREIGN KEY (album_id) REFERENCES album (id)
);

CREATE TABLE event_image (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             event_id BIGINT,
                             image_id BIGINT,
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             CONSTRAINT fk_event_image_event FOREIGN KEY (event_id) REFERENCES event(id),
                             CONSTRAINT fk_event_image_image FOREIGN KEY (image_id) REFERENCES image(id),
                             CONSTRAINT uk_event_image_event_id_image_id UNIQUE (event_id, image_id)
);

CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           member_id BIGINT NOT NULL,
                           album_id BIGINT NOT NULL,
                           status VARCHAR(255) NOT NULL CHECK (status IN ('INCLUDED','EXCLUDED')),
                           CONSTRAINT fk_favorites_member FOREIGN KEY (member_id) REFERENCES member (id),
                           CONSTRAINT fk_favorites_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE participant (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             member_id BIGINT NOT NULL,
                             album_id BIGINT NOT NULL,
                             role VARCHAR(255) NOT NULL CHECK (role IN ('HOST','STANDARD','LIMITED')),
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             CONSTRAINT fk_participant_member FOREIGN KEY (member_id) REFERENCES member (id),
                             CONSTRAINT fk_participant_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE notification (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             sender_id BIGINT NOT NULL,
                             receiver_id BIGINT NOT NULL,
                             album_id BIGINT,
                             title VARCHAR(30) NOT NULL,
                             content VARCHAR(100) NOT NULL,
                             type VARCHAR(20) NOT NULL CHECK (type IN ('ALBUM')),
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES member (id),
                             CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES member (id),
                             CONSTRAINT fk_notification_album FOREIGN KEY (album_id) REFERENCES album (id)
);
