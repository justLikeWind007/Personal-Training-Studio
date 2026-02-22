package com.jianshengfang.ptstudio.core.infrastructure.finance.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface MysqlFinanceMapper {

    @Insert("""
            INSERT INTO t_order(tenant_id, store_id, order_no, member_id, order_type,
                                total_amount, discount_amount, payable_amount, paid_amount, status, pay_status,
                                created_by, updated_by, is_deleted, created_at, updated_at)
            VALUES(#{tenantId}, #{storeId}, #{orderNo}, #{memberId}, #{orderType},
                   #{totalAmount}, 0, #{totalAmount}, #{paidAmount}, #{status}, 'UNPAID',
                   0, 0, 0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(MysqlFinancePo.OrderPo po);

    @Select("""
            SELECT id, tenant_id, store_id, order_no, member_id, order_type, total_amount, paid_amount,
                   status, created_at, updated_at
            FROM t_order
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            ORDER BY id
            """)
    List<MysqlFinancePo.OrderPo> listOrders(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, order_no, member_id, order_type, total_amount, paid_amount,
                   status, created_at, updated_at
            FROM t_order
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    MysqlFinancePo.OrderPo getOrder(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Update("""
            UPDATE t_order
            SET paid_amount = #{paidAmount}, status = #{status}, updated_at = #{updatedAt}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId} AND is_deleted = 0
            """)
    int updateOrderPaid(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("storeId") Long storeId,
                        @Param("paidAmount") BigDecimal paidAmount,
                        @Param("status") String status,
                        @Param("updatedAt") OffsetDateTime updatedAt);

    @Insert("""
            INSERT INTO t_payment_transaction(tenant_id, store_id, pay_no, order_id, pay_channel,
                                              out_trade_no, amount, pay_status, created_at, updated_at)
            VALUES(#{tenantId}, #{storeId}, #{payNo}, #{orderId}, #{payChannel},
                   #{outTradeNo}, #{amount}, #{payStatus}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPayment(MysqlFinancePo.PaymentPo po);

    @Select("""
            SELECT id, tenant_id, store_id, pay_no, order_id, pay_channel, out_trade_no, channel_trade_no,
                   amount, pay_status, CAST(callback_raw AS CHAR) AS callbackRaw, paid_at, created_at, updated_at
            FROM t_payment_transaction
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND channel_trade_no = #{channelTradeNo}
            LIMIT 1
            """)
    MysqlFinancePo.PaymentPo getPaymentByChannelTradeNo(@Param("tenantId") Long tenantId,
                                                        @Param("storeId") Long storeId,
                                                        @Param("channelTradeNo") String channelTradeNo);

    @Select("""
            SELECT id, tenant_id, store_id, pay_no, order_id, pay_channel, out_trade_no, channel_trade_no,
                   amount, pay_status, CAST(callback_raw AS CHAR) AS callbackRaw, paid_at, created_at, updated_at
            FROM t_payment_transaction
            WHERE order_id = #{orderId} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            ORDER BY id DESC LIMIT 1
            """)
    MysqlFinancePo.PaymentPo getLatestPaymentByOrder(@Param("orderId") Long orderId,
                                                     @Param("tenantId") Long tenantId,
                                                     @Param("storeId") Long storeId);

    @Update("""
            UPDATE t_payment_transaction
            SET channel_trade_no = #{channelTradeNo}, callback_raw = #{callbackRaw},
                pay_status = 'PAID', paid_at = #{paidAt}, updated_at = #{paidAt}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    int markPaymentPaid(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("storeId") Long storeId,
                        @Param("channelTradeNo") String channelTradeNo,
                        @Param("callbackRaw") String callbackRaw,
                        @Param("paidAt") OffsetDateTime paidAt);

    @Select("""
            SELECT id, tenant_id, store_id, pay_no, order_id, pay_channel, out_trade_no, channel_trade_no,
                   amount, pay_status, CAST(callback_raw AS CHAR) AS callbackRaw, paid_at, created_at, updated_at
            FROM t_payment_transaction
            WHERE order_id = #{orderId} AND tenant_id = #{tenantId} AND store_id = #{storeId}
              AND pay_status = 'PAID'
            ORDER BY id DESC LIMIT 1
            """)
    MysqlFinancePo.PaymentPo getLatestPaidPaymentByOrder(@Param("orderId") Long orderId,
                                                         @Param("tenantId") Long tenantId,
                                                         @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO t_refund(tenant_id, store_id, refund_no, order_id, payment_id, refund_amount,
                                 reason, status, created_by, created_at, updated_at)
            VALUES(#{tenantId}, #{storeId}, #{refundNo}, #{orderId}, #{paymentId}, #{refundAmount},
                   #{reason}, #{status}, 0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRefund(MysqlFinancePo.RefundPo po);

    @Select("""
            SELECT id, tenant_id, store_id, refund_no, order_id, payment_id, refund_amount, reason,
                   status, approved_by, approved_at, created_at, updated_at
            FROM t_refund
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    MysqlFinancePo.RefundPo getRefund(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Update("""
            UPDATE t_refund
            SET status = #{status}, approved_by = #{approvedBy}, approved_at = #{approvedAt}, updated_at = #{approvedAt}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    int updateRefundDecision(@Param("id") Long id,
                             @Param("tenantId") Long tenantId,
                             @Param("storeId") Long storeId,
                             @Param("status") String status,
                             @Param("approvedBy") Long approvedBy,
                             @Param("approvedAt") OffsetDateTime approvedAt);

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM t_payment_transaction
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
              AND pay_status = 'PAID' AND DATE(paid_at) = #{bizDate}
            """)
    BigDecimal sumPaidByDate(@Param("tenantId") Long tenantId,
                             @Param("storeId") Long storeId,
                             @Param("bizDate") LocalDate bizDate);

    @Select("""
            SELECT COALESCE(SUM(refund_amount), 0)
            FROM t_refund
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
              AND status = 'APPROVED' AND DATE(approved_at) = #{bizDate}
            """)
    BigDecimal sumApprovedRefundByDate(@Param("tenantId") Long tenantId,
                                       @Param("storeId") Long storeId,
                                       @Param("bizDate") LocalDate bizDate);
}
