CREATE TABLE member (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        nickname VARCHAR(50) NOT NULL,
                        oauth_id VARCHAR(255) NOT NULL,
                        oauth_provider VARCHAR(255),
                        profile VARCHAR(100) NOT NULL,
                        role VARCHAR(255) NOT NULL,
                        status VARCHAR(255) NOT NULL,
                        app_alarm BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL
);


CREATE TABLE subscription (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              member_id BIGINT UNIQUE NOT NULL,
                              subscription_type VARCHAR(255) NOT NULL,
                              subscription_status VARCHAR(255) NOT NULL,
                              start_at DATETIME(6),
                              end_at DATETIME(6),
                              next_billing_at DATETIME(6),
                              CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES member (id)
);


CREATE TABLE album (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       title VARCHAR(50) NOT NULL,
                       image_url VARCHAR(255) NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL
);


CREATE TABLE event (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       album_id BIGINT NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       CONSTRAINT fk_event_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE image (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       album_id BIGINT NOT NULL,
                       event_id BIGINT ,
                       url VARCHAR(255) NOT NULL,
                       image_file_created DATETIME,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       CONSTRAINT fk_image_album FOREIGN KEY (album_id) REFERENCES album (id),
                       CONSTRAINT fk_image_event FOREIGN KEY (event_id) REFERENCES event (id)
);


CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           member_id BIGINT NOT NULL,
                           album_id BIGINT NOT NULL,
                           CONSTRAINT fk_favorites_member FOREIGN KEY (member_id) REFERENCES member (id),
                           CONSTRAINT fk_favorites_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE participant (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             member_id BIGINT NOT NULL,
                             album_id BIGINT NOT NULL,
                             role VARCHAR(255) NOT NULL,
                             password VARCHAR(255),
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             CONSTRAINT fk_participant_member FOREIGN KEY (member_id) REFERENCES member (id),
                             CONSTRAINT fk_participant_album FOREIGN KEY (album_id) REFERENCES album (id)
);


CREATE TABLE payment (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         member_id BIGINT NOT NULL ,
                         merchant_uid VARCHAR(255) NOT NULL,
                         imp_uid VARCHAR(255),
                         pg_provider VARCHAR(255),
                         amount INT NOT NULL,
                         payment_status VARCHAR(255) NOT NULL,
                         paid_at DATETIME,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES member (id)
);
