package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class TouchRecordService {

    private final AtomicLong idGenerator = new AtomicLong(1L);
    private final Map<String, Map<Long, TouchRecord>> dataByKey = new ConcurrentHashMap<>();

    @Transactional
    public TouchRecord create(String tenantId,
                              String storeId,
                              Long memberId,
                              String taskNo,
                              String channel,
                              String contentSummary,
                              String result,
                              Long operatorUserId) {
        long id = idGenerator.getAndIncrement();
        TouchRecord record = new TouchRecord(
                id,
                tenantId,
                storeId,
                memberId,
                taskNo,
                channel,
                contentSummary,
                result,
                operatorUserId,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        dataByKey.computeIfAbsent(key(tenantId, storeId), ignored -> new ConcurrentHashMap<>())
                .put(id, record);
        return record;
    }

    public List<TouchRecord> list(String tenantId, String storeId, Long memberId, String taskNo) {
        return dataByKey.getOrDefault(key(tenantId, storeId), Map.of()).values().stream()
                .filter(record -> memberId == null || memberId.equals(record.memberId()))
                .filter(record -> taskNo == null || taskNo.equals(record.taskNo()))
                .sorted(Comparator.comparing(TouchRecord::executedAt).reversed())
                .toList();
    }

    public String exportCsv(String tenantId, String storeId, Long memberId, String taskNo) {
        List<TouchRecord> records = list(tenantId, storeId, memberId, taskNo);
        StringBuilder csv = new StringBuilder("id,memberId,taskNo,channel,contentSummary,result,operatorUserId,executedAt\\n");
        for (TouchRecord record : records) {
            csv.append(record.id()).append(',')
                    .append(record.memberId() == null ? "" : record.memberId()).append(',')
                    .append(escape(record.taskNo())).append(',')
                    .append(escape(record.channel())).append(',')
                    .append(escape(record.contentSummary())).append(',')
                    .append(escape(record.result())).append(',')
                    .append(record.operatorUserId()).append(',')
                    .append(escape(record.executedAt().toString()))
                    .append('\n');
        }
        return csv.toString();
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public record TouchRecord(Long id,
                              String tenantId,
                              String storeId,
                              Long memberId,
                              String taskNo,
                              String channel,
                              String contentSummary,
                              String result,
                              Long operatorUserId,
                              OffsetDateTime executedAt,
                              OffsetDateTime updatedAt) {
    }
}
