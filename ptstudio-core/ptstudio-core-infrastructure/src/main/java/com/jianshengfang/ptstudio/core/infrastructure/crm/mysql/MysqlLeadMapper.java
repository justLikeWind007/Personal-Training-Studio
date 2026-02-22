package com.jianshengfang.ptstudio.core.infrastructure.crm.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface MysqlLeadMapper {

    @Select("""
            SELECT id, tenant_id, store_id, lead_no, source, status, lead_name, mobile_hash,
                   owner_user_id, last_follow_at, next_follow_at, converted_member_id,
                   created_at, updated_at
            FROM t_lead
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            ORDER BY id
            """)
    List<MysqlLeadPo> list(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, lead_no, source, status, lead_name, mobile_hash,
                   owner_user_id, last_follow_at, next_follow_at, converted_member_id,
                   created_at, updated_at
            FROM t_lead
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    MysqlLeadPo get(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO t_lead(tenant_id, store_id, lead_no, source, status, lead_name, mobile_hash,
                               owner_user_id, next_follow_at, created_by, updated_by, is_deleted)
            VALUES(#{tenantId}, #{storeId}, #{leadNo}, #{source}, #{status}, #{leadName}, #{mobileHash},
                   #{ownerUserId}, #{nextFollowAt}, 0, 0, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MysqlLeadPo po);

    @Update("""
            UPDATE t_lead
            SET source = #{source}, status = #{status}, lead_name = #{leadName}, mobile_hash = #{mobileHash},
                owner_user_id = #{ownerUserId}, next_follow_at = #{nextFollowAt}, updated_by = 0
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    int update(MysqlLeadPo po);

    @Insert("""
            INSERT INTO t_lead_follow(tenant_id, store_id, lead_id, follow_type, content, next_follow_at, follower_user_id)
            VALUES(#{tenantId}, #{storeId}, #{leadId}, #{followType}, #{content}, #{nextFollowAt}, #{followerUserId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFollow(MysqlLeadFollowPo po);

    @Select("""
            SELECT id, tenant_id, store_id, lead_id, follow_type, content, next_follow_at, follower_user_id, created_at
            FROM t_lead_follow
            WHERE lead_id = #{leadId} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            ORDER BY id
            """)
    List<MysqlLeadFollowPo> listFollows(@Param("leadId") Long leadId,
                                        @Param("tenantId") Long tenantId,
                                        @Param("storeId") Long storeId);

    @Update("""
            UPDATE t_lead
            SET status = 'FOLLOWING', last_follow_at = #{lastFollowAt}, next_follow_at = #{nextFollowAt}, updated_by = 0
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    int updateAfterFollow(@Param("id") Long id,
                          @Param("tenantId") Long tenantId,
                          @Param("storeId") Long storeId,
                          @Param("lastFollowAt") OffsetDateTime lastFollowAt,
                          @Param("nextFollowAt") OffsetDateTime nextFollowAt);

    @Update("""
            UPDATE t_lead
            SET status = 'CONVERTED', converted_member_id = #{memberId}, updated_by = 0
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    int markConverted(@Param("id") Long id,
                      @Param("tenantId") Long tenantId,
                      @Param("storeId") Long storeId,
                      @Param("memberId") Long memberId);
}
