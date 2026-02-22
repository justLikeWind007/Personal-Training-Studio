package com.jianshengfang.ptstudio.core.infrastructure.schedule.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface MysqlScheduleMapper {

    @Select("""
            SELECT id, tenant_key, store_key, coach_name, mobile, coach_level, specialties, status, created_at, updated_at
            FROM t_app_coach_profile
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
            ORDER BY id
            """)
    List<MysqlSchedulePo.CoachPo> listCoaches(@Param("tenantKey") String tenantKey, @Param("storeKey") String storeKey);

    @Select("""
            SELECT id, tenant_key, store_key, coach_name, mobile, coach_level, specialties, status, created_at, updated_at
            FROM t_app_coach_profile
            WHERE id = #{id} AND tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    MysqlSchedulePo.CoachPo getCoach(@Param("id") Long id,
                                     @Param("tenantKey") String tenantKey,
                                     @Param("storeKey") String storeKey);

    @Insert("""
            INSERT INTO t_app_coach_profile(tenant_key, store_key, coach_name, mobile, coach_level, specialties, status)
            VALUES(#{tenantKey}, #{storeKey}, #{coachName}, #{mobile}, #{coachLevel}, #{specialties}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCoach(MysqlSchedulePo.CoachPo po);

    @Select("""
            SELECT id, tenant_key, store_key, coach_id, slot_date, start_time, end_time, capacity, booked_count, status, created_at, updated_at
            FROM t_app_schedule_slot
            WHERE coach_id = #{coachId} AND tenant_key = #{tenantKey} AND store_key = #{storeKey}
            ORDER BY slot_date, start_time
            """)
    List<MysqlSchedulePo.SlotPo> listCoachSlots(@Param("coachId") Long coachId,
                                                @Param("tenantKey") String tenantKey,
                                                @Param("storeKey") String storeKey);

    @Select("""
            SELECT id, tenant_key, store_key, coach_id, slot_date, start_time, end_time, capacity, booked_count, status, created_at, updated_at
            FROM t_app_schedule_slot
            WHERE id = #{id} AND tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    MysqlSchedulePo.SlotPo getSlot(@Param("id") Long id,
                                   @Param("tenantKey") String tenantKey,
                                   @Param("storeKey") String storeKey);

    @Insert("""
            INSERT INTO t_app_schedule_slot(tenant_key, store_key, coach_id, slot_date, start_time, end_time, capacity, booked_count, status)
            VALUES(#{tenantKey}, #{storeKey}, #{coachId}, #{slotDate}, #{startTime}, #{endTime}, #{capacity}, #{bookedCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSlot(MysqlSchedulePo.SlotPo po);

    @Update("""
            UPDATE t_app_schedule_slot
            SET booked_count = #{bookedCount}, updated_at = #{updatedAt}
            WHERE id = #{id} AND tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    int updateSlotBookedCount(@Param("id") Long id,
                              @Param("tenantKey") String tenantKey,
                              @Param("storeKey") String storeKey,
                              @Param("bookedCount") Integer bookedCount,
                              @Param("updatedAt") OffsetDateTime updatedAt);

    @Select("""
            SELECT id, tenant_key, store_key, coach_id, slot_date, start_time, end_time, capacity, booked_count, status, created_at, updated_at
            FROM t_app_schedule_slot
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
              AND status = 'OPEN' AND booked_count < capacity
              AND (#{coachId} IS NULL OR coach_id = #{coachId})
              AND (#{slotDate} IS NULL OR slot_date = #{slotDate})
            ORDER BY slot_date, start_time
            """)
    List<MysqlSchedulePo.SlotPo> listAvailableSlots(@Param("tenantKey") String tenantKey,
                                                    @Param("storeKey") String storeKey,
                                                    @Param("coachId") Long coachId,
                                                    @Param("slotDate") LocalDate slotDate);

    @Select("""
            SELECT COUNT(1)
            FROM t_app_reservation
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
              AND member_id = #{memberId} AND slot_id = #{slotId}
              AND status = 'BOOKED'
            """)
    long countBookedReservation(@Param("tenantKey") String tenantKey,
                                @Param("storeKey") String storeKey,
                                @Param("memberId") Long memberId,
                                @Param("slotId") Long slotId);

    @Insert("""
            INSERT INTO t_app_reservation(tenant_key, store_key, reservation_no, member_id, coach_id, slot_id, status, created_at, updated_at)
            VALUES(#{tenantKey}, #{storeKey}, #{reservationNo}, #{memberId}, #{coachId}, #{slotId}, #{status}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReservation(MysqlSchedulePo.ReservationPo po);

    @Select("""
            SELECT id, tenant_key, store_key, reservation_no, member_id, coach_id, slot_id, status,
                   cancel_reason, cancel_at, created_at, updated_at
            FROM t_app_reservation
            WHERE id = #{id} AND tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    MysqlSchedulePo.ReservationPo getReservation(@Param("id") Long id,
                                                 @Param("tenantKey") String tenantKey,
                                                 @Param("storeKey") String storeKey);

    @Select("""
            SELECT id, tenant_key, store_key, reservation_no, member_id, coach_id, slot_id, status,
                   cancel_reason, cancel_at, created_at, updated_at
            FROM t_app_reservation
            WHERE tenant_key = #{tenantKey} AND store_key = #{storeKey}
              AND (#{memberId} IS NULL OR member_id = #{memberId})
              AND (#{coachId} IS NULL OR coach_id = #{coachId})
              AND (#{status} IS NULL OR status = #{status})
            ORDER BY id
            """)
    List<MysqlSchedulePo.ReservationPo> listReservations(@Param("tenantKey") String tenantKey,
                                                         @Param("storeKey") String storeKey,
                                                         @Param("memberId") Long memberId,
                                                         @Param("coachId") Long coachId,
                                                         @Param("status") String status);

    @Update("""
            UPDATE t_app_reservation
            SET status = 'CANCELED', cancel_reason = #{cancelReason}, cancel_at = #{cancelAt}, updated_at = #{cancelAt}
            WHERE id = #{id} AND tenant_key = #{tenantKey} AND store_key = #{storeKey}
            """)
    int cancelReservation(@Param("id") Long id,
                          @Param("tenantKey") String tenantKey,
                          @Param("storeKey") String storeKey,
                          @Param("cancelReason") String cancelReason,
                          @Param("cancelAt") OffsetDateTime cancelAt);
}
