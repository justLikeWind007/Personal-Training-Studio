package com.jianshengfang.ptstudio.core.infrastructure.product.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MysqlProductAssetMapper {

    @Select("""
            SELECT id, tenant_id, store_id, package_code, package_name, total_sessions, valid_days,
                   price, sale_status, created_at, updated_at
            FROM t_package
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            ORDER BY id
            """)
    List<MysqlProductAssetPo.PackagePo> listPackages(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, package_code, package_name, total_sessions, valid_days,
                   price, sale_status, created_at, updated_at
            FROM t_package
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    MysqlProductAssetPo.PackagePo getPackage(@Param("id") Long id,
                                             @Param("tenantId") Long tenantId,
                                             @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO t_package(tenant_id, store_id, package_code, package_name, total_sessions,
                                  valid_days, price, sale_status, created_by, updated_by, is_deleted)
            VALUES(#{tenantId}, #{storeId}, #{packageCode}, #{packageName}, #{totalSessions},
                   #{validDays}, #{price}, #{saleStatus}, 0, 0, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPackage(MysqlProductAssetPo.PackagePo po);

    @Update("""
            UPDATE t_package
            SET package_name = #{packageName}, total_sessions = #{totalSessions}, valid_days = #{validDays},
                price = #{price}, sale_status = #{saleStatus}, updated_by = 0
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    int updatePackage(MysqlProductAssetPo.PackagePo po);

    @Select("""
            SELECT id, tenant_id, store_id, account_no, member_id, package_id, total_sessions,
                   used_sessions, remaining_sessions, expire_at, status, created_at, updated_at
            FROM t_member_package_account
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND member_id = #{memberId}
            ORDER BY id
            """)
    List<MysqlProductAssetPo.MemberPackagePo> listMemberPackages(@Param("tenantId") Long tenantId,
                                                                 @Param("storeId") Long storeId,
                                                                 @Param("memberId") Long memberId);

    @Insert("""
            INSERT INTO t_member_package_account(tenant_id, store_id, account_no, member_id, package_id,
                                                 source_order_id, total_sessions, used_sessions,
                                                 remaining_sessions, expire_at, status)
            VALUES(#{tenantId}, #{storeId}, #{accountNo}, #{memberId}, #{packageId},
                   0, #{totalSessions}, #{usedSessions}, #{remainingSessions}, #{expireAt}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMemberPackage(MysqlProductAssetPo.MemberPackagePo po);

    @Select("""
            SELECT l.id, l.tenant_id, l.store_id, l.account_id, l.action_type, l.sessions_delta,
                   l.before_sessions, l.after_sessions, l.biz_type, l.biz_id, l.operator_user_id, l.occurred_at
            FROM t_member_package_ledger l
            JOIN t_member_package_account a ON a.id = l.account_id
            WHERE l.tenant_id = #{tenantId} AND l.store_id = #{storeId} AND a.member_id = #{memberId}
            ORDER BY l.id
            """)
    List<MysqlProductAssetPo.LedgerPo> listMemberLedgers(@Param("tenantId") Long tenantId,
                                                         @Param("storeId") Long storeId,
                                                         @Param("memberId") Long memberId);

    @Insert("""
            INSERT INTO t_member_package_ledger(tenant_id, store_id, account_id, action_type, sessions_delta,
                                                before_sessions, after_sessions, biz_type, biz_id, operator_user_id)
            VALUES(#{tenantId}, #{storeId}, #{accountId}, #{actionType}, #{sessionsDelta},
                   #{beforeSessions}, #{afterSessions}, #{bizType}, #{bizId}, #{operatorUserId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLedger(MysqlProductAssetPo.LedgerPo po);
}
