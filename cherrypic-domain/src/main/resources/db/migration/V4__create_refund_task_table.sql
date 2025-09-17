CREATE TABLE refund_task (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         payment_id BIGINT NOT NULL,
                         status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','COMPLETED','SKIPPED','FAILED')),
                         scheduled_at DATETIME,
                         executed_at DATETIME,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL
);
