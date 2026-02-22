package com.jianshengfang.ptstudio.core.infrastructure.settings.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface MysqlStoreSettingsMapper {

    @Select("""
            SELECT id, tenant_key, store_key, store_name,
                   CAST(business_hours_json AS CHAR) AS businessHoursJson,
                   created_at, updated_at
            FROM t_app_store_settings
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
            LIMIT 1
            """)
    MysqlStoreSettingsPo get(@Param("tenantKey") String tenantKey, @Param("storeKey") String storeKey);

    @Insert("""
            INSERT INTO t_app_store_settings(tenant_key, store_key, store_name, business_hours_json, created_at, updated_at)
            VALUES(#{tenantKey}, #{storeKey}, #{storeName}, #{businessHoursJson}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MysqlStoreSettingsPo po);

    @Update("""
            UPDATE t_app_store_settings
            SET store_name = #{storeName}, business_hours_json = #{businessHoursJson}, updated_at = #{updatedAt}
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    int update(@Param("tenantKey") String tenantKey,
               @Param("storeKey") String storeKey,
               @Param("storeName") String storeName,
               @Param("businessHoursJson") String businessHoursJson,
               @Param("updatedAt") OffsetDateTime updatedAt);
}
