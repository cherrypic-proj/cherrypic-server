CREATE TABLE temp_album (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            member_id BIGINT NOT NULL,
                            title VARCHAR(20) NOT NULL,
                            capacity_gb DECIMAL(6,2) NOT NULL,
                            type VARCHAR(255) CHECK (type IN ('DEFAULT')),
                            expired_at DATETIME NOT NULL,
                            web_url VARCHAR(255),
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,

                            CONSTRAINT fk_temp_album_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE temp_album_image (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  temp_album_id BIGINT NOT NULL,
                                  url VARCHAR(255) NOT NULL,
                                  created_at DATETIME(6) NOT NULL,
                                  updated_at DATETIME(6) NOT NULL,
                                  CONSTRAINT fk_temp_album FOREIGN KEY (temp_album_id) REFERENCES temp_album(id)
);
