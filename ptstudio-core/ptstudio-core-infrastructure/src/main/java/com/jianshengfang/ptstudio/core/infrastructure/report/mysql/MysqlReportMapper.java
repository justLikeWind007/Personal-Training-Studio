package com.jianshengfang.ptstudio.core.infrastructure.report.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface MysqlReportMapper {

    @Select("""
            SELECT COUNT(DISTINCT member_id)
            FROM t_order
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    Long countDistinctMembers(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT COUNT(1)
            FROM t_app_reservation
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    Long countReservations(@Param("tenantKey") String tenantKey, @Param("storeKey") String storeKey);

    @Select("""
            SELECT COUNT(1)
            FROM t_app_reservation
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey} AND status = 'CANCELED'
            """)
    Long countCanceledReservations(@Param("tenantKey") String tenantKey, @Param("storeKey") String storeKey);

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM t_payment_transaction
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND pay_status = 'PAID'
            """)
    BigDecimal sumPaidRevenue(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM t_order
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    BigDecimal sumOrderAmount(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT COALESCE(SUM(refund_amount), 0)
            FROM t_refund
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND status = 'APPROVED'
            """)
    BigDecimal sumApprovedRefundAmount(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM t_commission_statement
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    BigDecimal sumCommissionAmount(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);
}
