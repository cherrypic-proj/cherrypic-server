CREATE TABLE refund_task (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         payment_id BIGINT NOT NULL,
                         status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','COMPLETED','FAILED')),
                         scheduled_at DATETIME,
                         executed_at DATETIME,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         CONSTRAINT fk_refund_task_payment FOREIGN KEY (payment_id) REFERENCES payment (id)
);
