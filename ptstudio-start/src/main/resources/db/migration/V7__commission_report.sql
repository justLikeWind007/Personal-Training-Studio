-- V7: 提成与报表（commission_report）
-- 依赖：V3, V5, V6

CREATE TABLE IF NOT EXISTS t_commission_rule (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  rule_code VARCHAR(32) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  calc_mode VARCHAR(32) NOT NULL,
  rule_json JSON NOT NULL,
  version INT NOT NULL,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_comm_rule_ver (tenant_id, store_id, rule_code, version),
  KEY idx_comm_rule_effective (tenant_id, store_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_commission_statement (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  statement_no VARCHAR(32) NOT NULL,
  statement_month CHAR(7) NOT NULL,
  coach_id BIGINT UNSIGNED NOT NULL,
  rule_id BIGINT UNSIGNED NOT NULL,
  gross_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  commission_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  adjust_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  final_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  locked_at DATETIME(3) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_statement_no (tenant_id, store_id, statement_no),
  UNIQUE KEY uk_statement_month_coach (tenant_id, store_id, statement_month, coach_id),
  KEY idx_statement_status (tenant_id, store_id, status, statement_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_commission_statement_item (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  statement_id BIGINT UNSIGNED NOT NULL,
  reservation_id BIGINT UNSIGNED NOT NULL,
  consumption_id BIGINT UNSIGNED NOT NULL,
  revenue_amount DECIMAL(12,2) NOT NULL,
  commission_rate DECIMAL(6,4) NOT NULL,
  commission_amount DECIMAL(12,2) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_statement_reservation (tenant_id, store_id, statement_id, reservation_id),
  KEY idx_statement_item_statement (tenant_id, store_id, statement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
