-- V8: 审计、幂等、Outbox与补偿（audit_idempotency_outbox_compensation）
-- 依赖：V1~V7

CREATE TABLE IF NOT EXISTS t_audit_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  module VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  action VARCHAR(64) NOT NULL,
  operator_user_id BIGINT UNSIGNED NOT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  trace_id VARCHAR(64) NULL,
  occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_audit_biz (tenant_id, store_id, biz_type, biz_id, occurred_at),
  KEY idx_audit_operator (tenant_id, store_id, operator_user_id, occurred_at),
  KEY idx_audit_module_time (tenant_id, store_id, module, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_idempotency_record (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  idem_key VARCHAR(128) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  response_code VARCHAR(32) NULL,
  response_json JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DONE',
  expired_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_idem_key (tenant_id, idem_key),
  KEY idx_idem_expired (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_outbox_event (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  topic VARCHAR(64) NOT NULL,
  tag VARCHAR(64) NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  payload_json JSON NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'NEW',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_outbox_event_id (event_id),
  KEY idx_outbox_dispatch (status, next_retry_at),
  KEY idx_outbox_biz (tenant_id, store_id, biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_compensation_task (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  task_no VARCHAR(32) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  task_payload JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  max_retry INT NOT NULL DEFAULT 20,
  next_retry_at DATETIME(3) NULL,
  last_error VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_comp_task_no (tenant_id, store_id, task_no),
  KEY idx_comp_dispatch (status, next_retry_at),
  KEY idx_comp_biz (tenant_id, store_id, biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
