package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
        long id = seq.getAndIncrement();
        QueuedEvent event = new QueuedEvent(
                id,
                taskNo,
                payload,
                0,
                maxRetry,
                "QUEUED",
                operatorUserId,
                OffsetDateTime.now(),
                null
        );
        queueByKey.computeIfAbsent(key(tenantId, storeId), ignored -> new ConcurrentLinkedQueue<>())
                .offer(event);
        return event;
    }

    @Transactional
    public ConsumeResult consume(String tenantId, String storeId, Integer batchSize, Long operatorUserId) {
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
        String key = key(tenantId, storeId);
        int queueSize = queueByKey.getOrDefault(key, new ConcurrentLinkedQueue<>()).size();
        int deadCount = deadByKey.getOrDefault(key, Map.of()).size();
        OffsetDateTime lastDispatchAt = lastDispatchByKey.get(key);
        String status = deadCount > 0 ? "DEGRADED" : "UP";
        return new HealthSnapshot(queueSize, deadCount, lastDispatchAt, status);
    }

    public List<DeadLetterEvent> deadLetters(String tenantId, String storeId) {
        return deadByKey.getOrDefault(key(tenantId, storeId), Map.of()).values().stream()
                .sorted(Comparator.comparing(DeadLetterEvent::failedAt).reversed())
                .toList();
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
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
