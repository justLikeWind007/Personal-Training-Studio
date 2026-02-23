package com.jianshengfang.ptstudio.core.infrastructure.outbox;

import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxEventPo;
import com.jianshengfang.ptstudio.core.infrastructure.schedule.mq.MysqlOutboxReservationEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("mq & mysql")
public class OutboxDispatchWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchWorker.class);

    private final MysqlOutboxReservationEventMapper outboxMapper;
    private final int batchSize;
    private final int maxRetry;
    private final int retryDelaySeconds;
    private final Set<String> failTags;

    public OutboxDispatchWorker(MysqlOutboxReservationEventMapper outboxMapper,
                                @Value("${ptstudio.messaging.outbox.batch-size:50}") int batchSize,
                                @Value("${ptstudio.messaging.outbox.max-retry:5}") int maxRetry,
                                @Value("${ptstudio.messaging.outbox.retry-delay-seconds:30}") int retryDelaySeconds,
                                @Value("${ptstudio.messaging.outbox.fail-tags:}") String failTagsRaw) {
        this.outboxMapper = outboxMapper;
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
        this.retryDelaySeconds = retryDelaySeconds;
        this.failTags = Arrays.stream(failTagsRaw.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    @Scheduled(fixedDelayString = "${ptstudio.messaging.outbox.dispatch-interval-ms:5000}")
    public void dispatch() {
        var events = outboxMapper.listDispatchable(OffsetDateTime.now(), batchSize);
        for (MysqlOutboxEventPo event : events) {
            try {
                send(event);
                outboxMapper.markSent(event.getId(), OffsetDateTime.now());
            } catch (Exception ex) {
                int currentRetry = event.getRetryCount() == null ? 0 : event.getRetryCount();
                if (currentRetry + 1 >= maxRetry) {
                    outboxMapper.markDead(event.getId(), OffsetDateTime.now());
                    log.error("outbox.dispatch.dead eventId={}, topic={}, tag={}, retries={}",
                            event.getEventId(), event.getTopic(), event.getTag(), currentRetry + 1, ex);
                } else {
                    OffsetDateTime nextRetryAt = OffsetDateTime.now().plusSeconds((long) retryDelaySeconds * (currentRetry + 1));
                    outboxMapper.markRetry(event.getId(), nextRetryAt, OffsetDateTime.now());
                    log.warn("outbox.dispatch.retry eventId={}, topic={}, tag={}, nextRetryAt={}",
                            event.getEventId(), event.getTopic(), event.getTag(), nextRetryAt, ex);
                }
            }
        }
    }

    private void send(MysqlOutboxEventPo event) {
        if (event.getTag() != null && failTags.contains(event.getTag())) {
            throw new IllegalStateException("outbox dispatch simulated failure for tag=" + event.getTag());
        }
        log.info("outbox.dispatch.sent eventId={}, topic={}, tag={}, bizType={}, bizId={}",
                event.getEventId(), event.getTopic(), event.getTag(), event.getBizType(), event.getBizId());
    }
}
