package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class OpsAsyncQueueService {

    private final AtomicLong seq = new AtomicLong(1L);
    private final Map<String, ConcurrentLinkedQueue<QueuedEvent>> queueByKey = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, DeadLetterEvent>> deadByKey = new ConcurrentHashMap<>();
    private final Map<String, OffsetDateTime> lastDispatchByKey = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;

    public OpsAsyncQueueService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                Environment environment) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.redisEnabled = environment.acceptsProfiles(Profiles.of("redis"));
    }

    @Transactional
    public QueuedEvent enqueueTaskEvent(String tenantId,
                                        String storeId,
                                        String taskNo,
                                        String payload,
                                        Long operatorUserId) {
        return enqueue(tenantId, storeId, taskNo, payload, 3, operatorUserId);
    }

    @Transactional
    public QueuedEvent enqueue(String tenantId,
                               String storeId,
                               String taskNo,
                               String payload,
                               Integer maxRetry,
                               Long operatorUserId) {
        QueuedEvent event = new QueuedEvent(
                seq.getAndIncrement(),
                taskNo,
                payload,
                0,
                maxRetry,
                "QUEUED",
                operatorUserId,
                OffsetDateTime.now(),
                null
        );

        if (useRedisQueue()) {
            enqueueToRedis(tenantId, storeId, event);
            return event;
        }

        queueByKey.computeIfAbsent(key(tenantId, storeId), ignored -> new ConcurrentLinkedQueue<>())
                .offer(event);
        return event;
    }

    @Transactional
    public ConsumeResult consume(String tenantId, String storeId, Integer batchSize, Long operatorUserId) {
        if (useRedisQueue()) {
            return consumeFromRedis(tenantId, storeId, batchSize, operatorUserId);
        }

        int size = Math.max(1, batchSize == null ? 10 : batchSize);
        ConcurrentLinkedQueue<QueuedEvent> queue = queueByKey.computeIfAbsent(
                key(tenantId, storeId), ignored -> new ConcurrentLinkedQueue<>());

        int success = 0;
        int retried = 0;
        int dead = 0;
        List<QueuedEvent> consumed = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            QueuedEvent current = queue.poll();
            if (current == null) {
                break;
            }
            QueuedEvent processing = current.withStatus("PROCESSING", operatorUserId);
            consumed.add(processing);

            boolean shouldFail = processing.payload() != null && processing.payload().contains("FAIL");
            if (!shouldFail) {
                success++;
                continue;
            }

            if (processing.retryCount() + 1 >= processing.maxRetry()) {
                dead++;
                DeadLetterEvent deadLetter = new DeadLetterEvent(
                        processing.id(),
                        processing.taskNo(),
                        processing.payload(),
                        processing.retryCount() + 1,
                        "MAX_RETRY_REACHED",
                        operatorUserId,
                        OffsetDateTime.now()
                );
                deadByKey.computeIfAbsent(key(tenantId, storeId), ignored -> new ConcurrentHashMap<>())
                        .put(deadLetter.id(), deadLetter);
            } else {
                retried++;
                queue.offer(processing.retry());
            }
        }

        lastDispatchByKey.put(key(tenantId, storeId), OffsetDateTime.now());
        return new ConsumeResult(success, retried, dead, consumed);
    }

    public HealthSnapshot health(String tenantId, String storeId) {
        if (useRedisQueue()) {
            String queueKey = streamKey(tenantId, storeId);
            String deadKey = deadStreamKey(tenantId, storeId);
            int queueSize = Math.toIntExact(redisTemplate.opsForStream().size(queueKey));
            int deadCount = Math.toIntExact(redisTemplate.opsForStream().size(deadKey));
            String lastDispatchRaw = redisTemplate.opsForValue().get(lastDispatchKey(tenantId, storeId));
            OffsetDateTime lastDispatchAt = lastDispatchRaw == null ? null : OffsetDateTime.parse(lastDispatchRaw);
            String status = deadCount > 0 ? "DEGRADED" : "UP";
            return new HealthSnapshot(queueSize, deadCount, lastDispatchAt, status);
        }

        String key = key(tenantId, storeId);
        int queueSize = queueByKey.getOrDefault(key, new ConcurrentLinkedQueue<>()).size();
        int deadCount = deadByKey.getOrDefault(key, Map.of()).size();
        OffsetDateTime lastDispatchAt = lastDispatchByKey.get(key);
        String status = deadCount > 0 ? "DEGRADED" : "UP";
        return new HealthSnapshot(queueSize, deadCount, lastDispatchAt, status);
    }

    public List<DeadLetterEvent> deadLetters(String tenantId, String storeId) {
        if (useRedisQueue()) {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .range(deadStreamKey(tenantId, storeId), Range.unbounded());
            return records.stream()
                    .map(record -> fromDeadMap(record.getValue()))
                    .sorted(Comparator.comparing(DeadLetterEvent::failedAt).reversed())
                    .toList();
        }

        return deadByKey.getOrDefault(key(tenantId, storeId), Map.of()).values().stream()
                .sorted(Comparator.comparing(DeadLetterEvent::failedAt).reversed())
                .toList();
    }

    private ConsumeResult consumeFromRedis(String tenantId, String storeId, Integer batchSize, Long operatorUserId) {
        int size = Math.max(1, batchSize == null ? 10 : batchSize);
        String queueKey = streamKey(tenantId, storeId);

        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(queueKey, Range.unbounded());
        int success = 0;
        int retried = 0;
        int dead = 0;
        List<QueuedEvent> consumed = new ArrayList<>();

        for (int i = 0; i < Math.min(size, records.size()); i++) {
            MapRecord<String, Object, Object> record = records.get(i);
            QueuedEvent current = fromQueuedMap(record.getValue());
            QueuedEvent processing = current.withStatus("PROCESSING", operatorUserId);
            consumed.add(processing);

            boolean shouldFail = processing.payload() != null && processing.payload().contains("FAIL");
            if (!shouldFail) {
                success++;
                redisTemplate.opsForStream().delete(queueKey, record.getId());
                continue;
            }

            if (processing.retryCount() + 1 >= processing.maxRetry()) {
                dead++;
                DeadLetterEvent deadLetter = new DeadLetterEvent(
                        processing.id(),
                        processing.taskNo(),
                        processing.payload(),
                        processing.retryCount() + 1,
                        "MAX_RETRY_REACHED",
                        operatorUserId,
                        OffsetDateTime.now()
                );
                enqueueDeadLetterToRedis(tenantId, storeId, deadLetter);
                redisTemplate.opsForStream().delete(queueKey, record.getId());
            } else {
                retried++;
                enqueueToRedis(tenantId, storeId, processing.retry());
                redisTemplate.opsForStream().delete(queueKey, record.getId());
            }
        }

        redisTemplate.opsForValue().set(lastDispatchKey(tenantId, storeId), OffsetDateTime.now().toString());
        return new ConsumeResult(success, retried, dead, consumed);
    }

    private void enqueueToRedis(String tenantId, String storeId, QueuedEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("id", String.valueOf(event.id()));
        fields.put("taskNo", event.taskNo());
        fields.put("payload", event.payload() == null ? "" : event.payload());
        fields.put("retryCount", String.valueOf(event.retryCount()));
        fields.put("maxRetry", String.valueOf(event.maxRetry()));
        fields.put("status", event.status());
        fields.put("operatorUserId", String.valueOf(event.operatorUserId()));
        fields.put("createdAt", event.createdAt().toString());
        RecordId ignored = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord().in(streamKey(tenantId, storeId)).ofMap(fields));
    }

    private void enqueueDeadLetterToRedis(String tenantId, String storeId, DeadLetterEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("id", String.valueOf(event.id()));
        fields.put("taskNo", event.taskNo());
        fields.put("payload", event.payload() == null ? "" : event.payload());
        fields.put("retryCount", String.valueOf(event.retryCount()));
        fields.put("reason", event.reason());
        fields.put("operatorUserId", String.valueOf(event.operatorUserId()));
        fields.put("failedAt", event.failedAt().toString());
        redisTemplate.opsForStream()
                .add(StreamRecords.newRecord().in(deadStreamKey(tenantId, storeId)).ofMap(fields));
    }

    private QueuedEvent fromQueuedMap(Map<Object, Object> map) {
        return new QueuedEvent(
                parseLong(map.get("id")),
                str(map.get("taskNo")),
                str(map.get("payload")),
                parseInt(map.get("retryCount")),
                parseInt(map.get("maxRetry")),
                str(map.get("status")),
                parseLong(map.get("operatorUserId")),
                OffsetDateTime.parse(str(map.get("createdAt"))),
                null
        );
    }

    private DeadLetterEvent fromDeadMap(Map<Object, Object> map) {
        return new DeadLetterEvent(
                parseLong(map.get("id")),
                str(map.get("taskNo")),
                str(map.get("payload")),
                parseInt(map.get("retryCount")),
                str(map.get("reason")),
                parseLong(map.get("operatorUserId")),
                OffsetDateTime.parse(str(map.get("failedAt")))
        );
    }

    private boolean useRedisQueue() {
        return redisEnabled && redisTemplate != null;
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
    }

    private String streamKey(String tenantId, String storeId) {
        return "ptstudio:ops:async:queue:" + tenantId + ":" + storeId;
    }

    private String deadStreamKey(String tenantId, String storeId) {
        return "ptstudio:ops:async:dead:" + tenantId + ":" + storeId;
    }

    private String lastDispatchKey(String tenantId, String storeId) {
        return "ptstudio:ops:async:last-dispatch:" + tenantId + ":" + storeId;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long parseLong(Object value) {
        return Long.parseLong(str(value));
    }

    private Integer parseInt(Object value) {
        return Integer.parseInt(str(value));
    }

    public record QueuedEvent(Long id,
                              String taskNo,
                              String payload,
                              Integer retryCount,
                              Integer maxRetry,
                              String status,
                              Long operatorUserId,
                              OffsetDateTime createdAt,
                              OffsetDateTime handledAt) {
        private QueuedEvent withStatus(String status, Long operatorUserId) {
            return new QueuedEvent(id, taskNo, payload, retryCount, maxRetry, status,
                    operatorUserId, createdAt, OffsetDateTime.now());
        }

        private QueuedEvent retry() {
            return new QueuedEvent(id, taskNo, payload, retryCount + 1, maxRetry, "QUEUED",
                    operatorUserId, createdAt, null);
        }
    }

    public record DeadLetterEvent(Long id,
                                  String taskNo,
                                  String payload,
                                  Integer retryCount,
                                  String reason,
                                  Long operatorUserId,
                                  OffsetDateTime failedAt) {
    }

    public record ConsumeResult(Integer successCount,
                                Integer retryCount,
                                Integer deadCount,
                                List<QueuedEvent> consumedEvents) {
    }

    public record HealthSnapshot(Integer queueSize,
                                 Integer deadLetterCount,
                                 OffsetDateTime lastDispatchAt,
                                 String status) {
    }
}
