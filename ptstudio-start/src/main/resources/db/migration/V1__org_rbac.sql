-- V1: 组织与权限（org_rbac）
-- 依赖：无

CREATE TABLE IF NOT EXISTS t_tenant (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(32) NOT NULL,
  tenant_name VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_store (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_code VARCHAR(32) NOT NULL,
  store_name VARCHAR(128) NOT NULL,
  business_hours_json JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_tenant_store_code (tenant_id, store_code),
  KEY idx_store_tenant (tenant_id),
  CONSTRAINT fk_store_tenant FOREIGN KEY (tenant_id) REFERENCES t_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  username VARCHAR(64) NOT NULL,
  mobile_enc VARBINARY(512) NULL,
  mobile_hash CHAR(64) NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME(3) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  UNIQUE KEY uk_tenant_mobile_hash (tenant_id, mobile_hash),
  KEY idx_user_tenant_store (tenant_id, store_id, status),
  CONSTRAINT fk_user_store FOREIGN KEY (store_id) REFERENCES t_store(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_role (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  role_key VARCHAR(64) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_role_key (tenant_id, store_id, role_key),
  KEY idx_role_tenant_store (tenant_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_permission (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  permission_key VARCHAR(128) NOT NULL,
  permission_name VARCHAR(128) NOT NULL,
  module VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_permission_key (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_role_permission (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_role_perm (tenant_id, store_id, role_id, permission_id),
  KEY idx_rp_role (role_id),
  KEY idx_rp_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_user_role (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user_role (tenant_id, store_id, user_id, role_id),
  KEY idx_ur_user (user_id),
  KEY idx_ur_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
