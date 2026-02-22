-- V3: 产品与会员资产（product_asset）
-- 依赖：V1, V2

CREATE TABLE IF NOT EXISTS t_coach_profile (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  coach_level VARCHAR(32) NULL,
  specialties VARCHAR(255) NULL,
  intro VARCHAR(1000) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_coach_user (tenant_id, store_id, user_id),
  KEY idx_coach_status (tenant_id, store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_package (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  package_code VARCHAR(32) NOT NULL,
  package_name VARCHAR(128) NOT NULL,
  total_sessions INT NOT NULL,
  valid_days INT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  sale_status VARCHAR(16) NOT NULL DEFAULT 'ON',
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_package_code (tenant_id, store_id, package_code),
  KEY idx_package_status (tenant_id, store_id, sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_member_package_account (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  account_no VARCHAR(32) NOT NULL,
  member_id BIGINT UNSIGNED NOT NULL,
  package_id BIGINT UNSIGNED NOT NULL,
  source_order_id BIGINT UNSIGNED NOT NULL,
  total_sessions INT NOT NULL,
  used_sessions INT NOT NULL DEFAULT 0,
  remaining_sessions INT NOT NULL,
  expire_at DATETIME(3) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  row_version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_mpa_no (tenant_id, store_id, account_no),
  KEY idx_mpa_member_status (tenant_id, store_id, member_id, status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_member_package_ledger (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  account_id BIGINT UNSIGNED NOT NULL,
  action_type VARCHAR(16) NOT NULL,
  sessions_delta INT NOT NULL,
  before_sessions INT NOT NULL,
  after_sessions INT NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  operator_user_id BIGINT UNSIGNED NULL,
  occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_mpl_biz (tenant_id, store_id, biz_type, biz_id, action_type),
  KEY idx_mpl_account_time (tenant_id, store_id, account_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
