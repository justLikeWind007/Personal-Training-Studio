package com.jianshengfang.ptstudio.core.infrastructure.crm.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MysqlMemberMapper {

    @Select("""
            SELECT id, tenant_id, store_id, member_no, member_name, mobile_hash, join_date,
                   level_tag, status, lead_id, created_at, updated_at
            FROM t_member
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            ORDER BY id
            """)
    List<MysqlMemberPo> list(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, member_no, member_name, mobile_hash, join_date,
                   level_tag, status, lead_id, created_at, updated_at
            FROM t_member
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    MysqlMemberPo get(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO t_member(tenant_id, store_id, member_no, member_name, mobile_hash, join_date,
                                 level_tag, status, lead_id, created_by, updated_by, is_deleted)
            VALUES(#{tenantId}, #{storeId}, #{memberNo}, #{memberName}, #{mobileHash}, #{joinDate},
                   #{levelTag}, #{status}, #{leadId}, 0, 0, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MysqlMemberPo po);

    @Update("""
            UPDATE t_member
            SET member_name = #{memberName}, mobile_hash = #{mobileHash}, level_tag = #{levelTag},
                status = #{status}, updated_by = 0
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    int update(MysqlMemberPo po);
}
