ALTER TABLE t_app_store_settings
  ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER business_hours_json;

CREATE INDEX idx_app_store_tenant_status ON t_app_store_settings(tenant_key, status);
