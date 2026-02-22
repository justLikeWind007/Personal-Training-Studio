-- V5: 签到课消与审批（checkin_consumption_approval）
-- 依赖：V2, V3, V4

CREATE TABLE IF NOT EXISTS t_checkin_record (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  checkin_no VARCHAR(32) NOT NULL,
  reservation_id BIGINT UNSIGNED NOT NULL,
  member_id BIGINT UNSIGNED NOT NULL,
  checkin_time DATETIME(3) NOT NULL,
  checkin_channel VARCHAR(16) NOT NULL,
  operator_user_id BIGINT UNSIGNED NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DONE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_checkin_no (tenant_id, store_id, checkin_no),
  UNIQUE KEY uk_checkin_reservation (tenant_id, store_id, reservation_id),
  KEY idx_checkin_member_time (tenant_id, store_id, member_id, checkin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_approval_request (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  reason VARCHAR(500) NULL,
  submitted_by BIGINT UNSIGNED NOT NULL,
  submitted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  approved_by BIGINT UNSIGNED NULL,
  approved_at DATETIME(3) NULL,
  reject_reason VARCHAR(500) NULL,
  UNIQUE KEY uk_approval_biz (tenant_id, store_id, biz_type, biz_id),
  KEY idx_approval_status (tenant_id, store_id, status, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_lesson_consumption (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  consumption_no VARCHAR(32) NOT NULL,
  reservation_id BIGINT UNSIGNED NOT NULL,
  member_package_account_id BIGINT UNSIGNED NOT NULL,
  action_type VARCHAR(16) NOT NULL,
  sessions_delta INT NOT NULL,
  consume_time DATETIME(3) NOT NULL,
  operator_user_id BIGINT UNSIGNED NULL,
  idem_key VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_consumption_no (tenant_id, store_id, consumption_no),
  UNIQUE KEY uk_consumption_idem (tenant_id, idem_key),
  KEY idx_consumption_reservation (tenant_id, store_id, reservation_id),
  KEY idx_consumption_account_time (tenant_id, store_id, member_package_account_id, consume_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
