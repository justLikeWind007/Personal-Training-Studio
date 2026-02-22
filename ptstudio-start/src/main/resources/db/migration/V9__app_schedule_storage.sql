CREATE TABLE IF NOT EXISTS t_app_coach_profile (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_key VARCHAR(64) NOT NULL,
  store_key VARCHAR(64) NOT NULL,
  coach_name VARCHAR(64) NOT NULL,
  mobile VARCHAR(32) NOT NULL,
  coach_level VARCHAR(32) NOT NULL,
  specialties VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_app_coach_tenant_store (tenant_key, store_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_app_schedule_slot (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_key VARCHAR(64) NOT NULL,
  store_key VARCHAR(64) NOT NULL,
  coach_id BIGINT UNSIGNED NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  capacity INT NOT NULL,
  booked_count INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_app_slot_unique (tenant_key, store_key, coach_id, slot_date, start_time, end_time),
  KEY idx_app_slot_query (tenant_key, store_key, slot_date, status, coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_app_reservation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_key VARCHAR(64) NOT NULL,
  store_key VARCHAR(64) NOT NULL,
  reservation_no VARCHAR(64) NOT NULL,
  member_id BIGINT UNSIGNED NOT NULL,
  coach_id BIGINT UNSIGNED NOT NULL,
  slot_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL,
  cancel_reason VARCHAR(255) NULL,
  cancel_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_app_reservation_no (tenant_key, store_key, reservation_no),
  UNIQUE KEY uk_app_member_slot (tenant_key, store_key, member_id, slot_id),
  KEY idx_app_reservation_query (tenant_key, store_key, status, coach_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
