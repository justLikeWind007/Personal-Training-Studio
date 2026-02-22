-- V4: 预约排班（schedule_reservation）
-- 依赖：V1, V2, V3

CREATE TABLE IF NOT EXISTS t_schedule_slot (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  coach_id BIGINT UNSIGNED NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  capacity INT NOT NULL DEFAULT 1,
  booked_count INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  row_version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_slot_unique (tenant_id, store_id, coach_id, slot_date, start_time, end_time),
  KEY idx_slot_query (tenant_id, store_id, slot_date, status, coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_reservation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  reservation_no VARCHAR(32) NOT NULL,
  member_id BIGINT UNSIGNED NOT NULL,
  coach_id BIGINT UNSIGNED NOT NULL,
  slot_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL,
  cancel_deadline_at DATETIME(3) NULL,
  cancel_reason VARCHAR(255) NULL,
  cancel_at DATETIME(3) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_reservation_no (tenant_id, store_id, reservation_no),
  UNIQUE KEY uk_member_slot (tenant_id, store_id, member_id, slot_id),
  KEY idx_reservation_slot (tenant_id, store_id, slot_id, status),
  KEY idx_reservation_member (tenant_id, store_id, member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
