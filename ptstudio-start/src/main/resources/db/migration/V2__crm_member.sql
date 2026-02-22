-- V2: CRM与会员（crm_member）
-- 依赖：V1__org_rbac.sql

CREATE TABLE IF NOT EXISTS t_lead (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  lead_no VARCHAR(32) NOT NULL,
  source VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  lead_name VARCHAR(64) NOT NULL,
  mobile_enc VARBINARY(512) NULL,
  mobile_hash CHAR(64) NULL,
  owner_user_id BIGINT UNSIGNED NOT NULL,
  last_follow_at DATETIME(3) NULL,
  next_follow_at DATETIME(3) NULL,
  converted_member_id BIGINT UNSIGNED NULL,
  remark VARCHAR(500) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_lead_no (tenant_id, store_id, lead_no),
  KEY idx_lead_owner_status (tenant_id, store_id, owner_user_id, status),
  KEY idx_lead_next_follow (tenant_id, store_id, next_follow_at),
  KEY idx_lead_mobile_hash (tenant_id, mobile_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_lead_follow (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  lead_id BIGINT UNSIGNED NOT NULL,
  follow_type VARCHAR(32) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  next_follow_at DATETIME(3) NULL,
  follower_user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_lf_lead_time (tenant_id, store_id, lead_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_member (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  member_no VARCHAR(32) NOT NULL,
  member_name VARCHAR(64) NOT NULL,
  mobile_enc VARBINARY(512) NULL,
  mobile_hash CHAR(64) NULL,
  gender VARCHAR(8) NULL,
  birthday DATE NULL,
  join_date DATE NOT NULL,
  level_tag VARCHAR(32) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  lead_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_no (tenant_id, store_id, member_no),
  KEY idx_member_mobile_hash (tenant_id, mobile_hash),
  KEY idx_member_status (tenant_id, store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
