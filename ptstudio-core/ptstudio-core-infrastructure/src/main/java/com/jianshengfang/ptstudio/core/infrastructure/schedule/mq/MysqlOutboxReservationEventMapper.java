package com.jianshengfang.ptstudio.core.infrastructure.schedule.mq;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

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
}
