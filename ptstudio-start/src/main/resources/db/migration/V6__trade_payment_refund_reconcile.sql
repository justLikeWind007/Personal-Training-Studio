-- V6: 交易支付退款对账（trade_payment_refund_reconcile）
-- 依赖：V2, V3

CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  member_id BIGINT UNSIGNED NOT NULL,
  order_type VARCHAR(32) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(12,2) NOT NULL,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL,
  pay_status VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
  row_version INT NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no (tenant_id, store_id, order_no),
  KEY idx_order_member (tenant_id, store_id, member_id, created_at),
  KEY idx_order_status_time (tenant_id, store_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_order_item (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  order_id BIGINT UNSIGNED NOT NULL,
  item_type VARCHAR(32) NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  qty INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_order_item_order (tenant_id, store_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_payment_transaction (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  pay_no VARCHAR(32) NOT NULL,
  order_id BIGINT UNSIGNED NOT NULL,
  pay_channel VARCHAR(16) NOT NULL,
  out_trade_no VARCHAR(64) NOT NULL,
  channel_trade_no VARCHAR(64) NULL,
  amount DECIMAL(12,2) NOT NULL,
  pay_status VARCHAR(16) NOT NULL DEFAULT 'INIT',
  paid_at DATETIME(3) NULL,
  callback_idem_key VARCHAR(128) NULL,
  callback_raw JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_pay_no (tenant_id, store_id, pay_no),
  UNIQUE KEY uk_out_trade_no (tenant_id, out_trade_no),
  UNIQUE KEY uk_channel_trade_no (tenant_id, channel_trade_no),
  UNIQUE KEY uk_callback_idem (tenant_id, callback_idem_key),
  KEY idx_pay_order_status (tenant_id, store_id, order_id, pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_refund (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  refund_no VARCHAR(32) NOT NULL,
  order_id BIGINT UNSIGNED NOT NULL,
  payment_id BIGINT UNSIGNED NOT NULL,
  refund_amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(255) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'APPLIED',
  approval_id BIGINT UNSIGNED NULL,
  approved_by BIGINT UNSIGNED NULL,
  approved_at DATETIME(3) NULL,
  channel_refund_no VARCHAR(64) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_refund_no (tenant_id, store_id, refund_no),
  UNIQUE KEY uk_channel_refund_no (tenant_id, channel_refund_no),
  KEY idx_refund_order_status (tenant_id, store_id, order_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_reconcile_daily (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  biz_date DATE NOT NULL,
  channel VARCHAR(16) NOT NULL,
  expected_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  actual_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  diff_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'INIT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_reconcile_day (tenant_id, store_id, biz_date, channel),
  KEY idx_reconcile_status (tenant_id, store_id, status, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_reconcile_exception (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT UNSIGNED NOT NULL,
  store_id BIGINT UNSIGNED NOT NULL,
  reconcile_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  diff_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  compensate_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_rex_reconcile (tenant_id, store_id, reconcile_id),
  KEY idx_rex_status (tenant_id, store_id, status, compensate_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
