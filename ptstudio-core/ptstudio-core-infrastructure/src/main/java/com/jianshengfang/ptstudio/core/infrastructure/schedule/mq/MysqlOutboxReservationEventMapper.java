package com.jianshengfang.ptstudio.core.infrastructure.schedule.mq;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface MysqlOutboxReservationEventMapper {

    @Insert("""
            INSERT INTO t_outbox_event(
                tenant_id, store_id, event_id, topic, tag, biz_type, biz_id, payload_json, status, retry_count
            ) VALUES(
                #{tenantId}, #{storeId}, #{eventId}, #{topic}, #{tag}, #{bizType}, #{bizId},
                CAST(#{payloadJson} AS JSON), 'NEW', 0
            )
            """)
    int insert(MysqlOutboxEventPo po);

    @Select("""
            SELECT id, tenant_id, store_id, event_id, topic, tag, biz_type, biz_id,
                   CAST(payload_json AS CHAR) AS payloadJson, status, retry_count, next_retry_at,
                   created_at, updated_at
            FROM t_outbox_event
            WHERE status IN ('NEW', 'RETRY')
              AND (next_retry_at IS NULL OR next_retry_at <= #{now})
            ORDER BY id
            LIMIT #{limit}
            """)
    List<MysqlOutboxEventPo> listDispatchable(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'SENT', updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int markSent(@Param("id") Long id, @Param("updatedAt") OffsetDateTime updatedAt);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'RETRY', retry_count = retry_count + 1, next_retry_at = #{nextRetryAt}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int markRetry(@Param("id") Long id,
                  @Param("nextRetryAt") OffsetDateTime nextRetryAt,
                  @Param("updatedAt") OffsetDateTime updatedAt);

    @Update("""
            UPDATE t_outbox_event
            SET status = 'DEAD', retry_count = retry_count + 1, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int markDead(@Param("id") Long id, @Param("updatedAt") OffsetDateTime updatedAt);
}
