package com.jianshengfang.ptstudio.core.infrastructure.attendance.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface MysqlAttendanceMapper {

    @Select("""
            SELECT COUNT(1)
            FROM t_checkin_record
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND reservation_id = #{reservationId}
              AND status = 'DONE'
            """)
    long countCheckinByReservation(@Param("tenantId") Long tenantId,
                                   @Param("storeId") Long storeId,
                                   @Param("reservationId") Long reservationId);

    @Insert("""
            INSERT INTO t_checkin_record(tenant_id, store_id, checkin_no, reservation_id, member_id,
                                         checkin_time, checkin_channel, operator_user_id, status, created_at)
            VALUES(#{tenantId}, #{storeId}, #{checkinNo}, #{reservationId}, #{memberId},
                   #{checkinTime}, #{checkinChannel}, #{operatorUserId}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCheckin(MysqlAttendancePo.CheckinPo po);

    @Select("""
            SELECT id, tenant_id, store_id, checkin_no, reservation_id, member_id,
                   checkin_time, checkin_channel, operator_user_id, status, created_at
            FROM t_checkin_record
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
            ORDER BY id
            """)
    List<MysqlAttendancePo.CheckinPo> listCheckins(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, consumption_no, reservation_id, member_package_account_id,
                   action_type, sessions_delta, consume_time, operator_user_id, idem_key, status, created_at
            FROM t_lesson_consumption
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND idem_key = #{idemKey}
            LIMIT 1
            """)
    MysqlAttendancePo.ConsumptionPo getConsumptionByIdemKey(@Param("tenantId") Long tenantId,
                                                            @Param("storeId") Long storeId,
                                                            @Param("idemKey") String idemKey);

    @Insert("""
            INSERT INTO t_lesson_consumption(tenant_id, store_id, consumption_no, reservation_id,
                                             member_package_account_id, action_type, sessions_delta,
                                             consume_time, operator_user_id, idem_key, status, created_at)
            VALUES(#{tenantId}, #{storeId}, #{consumptionNo}, #{reservationId},
                   #{memberPackageAccountId}, #{actionType}, #{sessionsDelta},
                   #{consumeTime}, #{operatorUserId}, #{idemKey}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertConsumption(MysqlAttendancePo.ConsumptionPo po);

    @Select("""
            SELECT id, tenant_id, store_id, consumption_no, reservation_id, member_package_account_id,
                   action_type, sessions_delta, consume_time, operator_user_id, idem_key, status, created_at
            FROM t_lesson_consumption
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
            ORDER BY id
            """)
    List<MysqlAttendancePo.ConsumptionPo> listConsumptions(@Param("tenantId") Long tenantId,
                                                           @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, consumption_no, reservation_id, member_package_account_id,
                   action_type, sessions_delta, consume_time, operator_user_id, idem_key, status, created_at
            FROM t_lesson_consumption
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    MysqlAttendancePo.ConsumptionPo getConsumption(@Param("id") Long id,
                                                   @Param("tenantId") Long tenantId,
                                                   @Param("storeId") Long storeId);

    @Update("""
            UPDATE t_lesson_consumption
            SET status = #{status}, operator_user_id = #{operatorUserId}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    int updateConsumptionStatus(@Param("id") Long id,
                                @Param("tenantId") Long tenantId,
                                @Param("storeId") Long storeId,
                                @Param("status") String status,
                                @Param("operatorUserId") Long operatorUserId);

    @Select("""
            SELECT COUNT(1)
            FROM t_checkin_record
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    Long countCheckins(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT COUNT(1)
            FROM t_lesson_consumption
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND status = 'SUCCESS'
            """)
    Long countSuccessConsumptions(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO t_approval_request(tenant_id, store_id, biz_type, biz_id, status, reason, submitted_by, submitted_at)
            VALUES(#{tenantId}, #{storeId}, #{bizType}, #{bizId}, #{status}, #{reason}, #{submittedBy}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertApproval(MysqlAttendancePo.ApprovalPo po);

    @Select("""
            SELECT id, tenant_id, store_id, biz_type, biz_id, status, reason, submitted_by, submitted_at,
                   approved_by, approved_at, reject_reason
            FROM t_approval_request
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId} AND biz_type = #{bizType} AND biz_id = #{bizId}
            LIMIT 1
            """)
    MysqlAttendancePo.ApprovalPo getApprovalByBiz(@Param("tenantId") Long tenantId,
                                                  @Param("storeId") Long storeId,
                                                  @Param("bizType") String bizType,
                                                  @Param("bizId") Long bizId);

    @Select("""
            SELECT id, tenant_id, store_id, biz_type, biz_id, status, reason, submitted_by, submitted_at,
                   approved_by, approved_at, reject_reason
            FROM t_approval_request
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            LIMIT 1
            """)
    MysqlAttendancePo.ApprovalPo getApproval(@Param("id") Long id,
                                             @Param("tenantId") Long tenantId,
                                             @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, biz_type, biz_id, status, reason, submitted_by, submitted_at,
                   approved_by, approved_at, reject_reason
            FROM t_approval_request
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
              AND biz_type = #{bizType}
              AND (#{status} IS NULL OR status = #{status})
            ORDER BY id
            """)
    List<MysqlAttendancePo.ApprovalPo> listApprovals(@Param("tenantId") Long tenantId,
                                                     @Param("storeId") Long storeId,
                                                     @Param("bizType") String bizType,
                                                     @Param("status") String status);

    @Update("""
            UPDATE t_approval_request
            SET status = #{status},
                approved_by = #{approvedBy},
                approved_at = #{approvedAt},
                reject_reason = #{rejectReason}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    int updateApproval(@Param("id") Long id,
                       @Param("tenantId") Long tenantId,
                       @Param("storeId") Long storeId,
                       @Param("status") String status,
                       @Param("approvedBy") Long approvedBy,
                       @Param("approvedAt") OffsetDateTime approvedAt,
                       @Param("rejectReason") String rejectReason);
}
