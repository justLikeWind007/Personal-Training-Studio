package com.jianshengfang.ptstudio.core.app.ops;

import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class OperationTaskService {

    private final MemberService memberService;
    private final OpsAsyncQueueService opsAsyncQueueService;
    private final Map<String, TaskRule> ruleByKey = new ConcurrentHashMap<>();
    private final Map<String, Map<String, OperationTask>> taskByNoByKey = new ConcurrentHashMap<>();
    private final AtomicLong taskSeq = new AtomicLong(0L);

    public OperationTaskService(MemberService memberService,
                                OpsAsyncQueueService opsAsyncQueueService) {
        this.memberService = memberService;
        this.opsAsyncQueueService = opsAsyncQueueService;
    }

    @Transactional
    public TaskRule saveRule(String tenantId,
                             String storeId,
                             String triggerType,
                             String priority,
                             String ownerRole,
                             String titleTemplate,
                             Integer generateLimit,
                             Long operatorUserId) {
        TaskRule rule = new TaskRule(
                tenantId,
                storeId,
                triggerType,
                priority,
                ownerRole,
                titleTemplate,
                generateLimit,
                operatorUserId,
                OffsetDateTime.now()
        );
        ruleByKey.put(key(tenantId, storeId), rule);
        return rule;
    }

    @Transactional
    public GenerateResult generate(String tenantId, String storeId, Long operatorUserId) {
        TaskRule rule = requireRule(tenantId, storeId);
        int limit = Math.max(1, rule.generateLimit());

        List<OperationTask> created = new ArrayList<>();
        List<com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore.MemberData> members =
                memberService.list(tenantId, storeId, "ACTIVE", null, null);

        for (com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore.MemberData member : members) {
            if (created.size() >= limit) {
                break;
            }
            String taskNo = "OT" + taskSeq.incrementAndGet();
            String title = rule.titleTemplate().replace("{memberName}", member.name());
            OperationTask task = new OperationTask(
                    taskNo,
                    tenantId,
                    storeId,
                    rule.triggerType(),
                    rule.priority(),
                    rule.ownerRole(),
                    title,
                    "TODO",
                    member.id(),
                    "AUTO_GENERATED",
                    null,
                    null,
                    operatorUserId,
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
            );
            putTask(task);
            opsAsyncQueueService.enqueueTaskEvent(tenantId, storeId, task.taskNo(), task.title(), operatorUserId);
            created.add(task);
        }

        if (created.isEmpty()) {
            String taskNo = "OT" + taskSeq.incrementAndGet();
            OperationTask fallback = new OperationTask(
                    taskNo,
                    tenantId,
                    storeId,
                    rule.triggerType(),
                    rule.priority(),
                    rule.ownerRole(),
                    "门店运营巡检",
                    "TODO",
                    null,
                    "AUTO_GENERATED",
                    null,
                    null,
                    operatorUserId,
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
            );
            putTask(fallback);
            opsAsyncQueueService.enqueueTaskEvent(tenantId, storeId, fallback.taskNo(), fallback.title(), operatorUserId);
            created.add(fallback);
        }

        return new GenerateResult(rule, created.size(), created);
    }

    @Transactional
    public OperationTask createFromJourney(String tenantId,
                                           String storeId,
                                           String nodeType,
                                           Long memberId,
                                           String title,
                                           String priority,
                                           String ownerRole,
                                           Long operatorUserId) {
        String taskNo = "OT" + taskSeq.incrementAndGet();
        OperationTask task = new OperationTask(
                taskNo,
                tenantId,
                storeId,
                nodeType,
                priority,
                ownerRole,
                title,
                "TODO",
                memberId,
                "JOURNEY_NODE",
                "AUTO_CREATE",
                operatorUserId,
                operatorUserId,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        putTask(task);
        opsAsyncQueueService.enqueueTaskEvent(tenantId, storeId, task.taskNo(), task.title(), operatorUserId);
        return task;
    }

    public List<OperationTask> list(String tenantId, String storeId, String status) {
        return taskByNoByKey.getOrDefault(key(tenantId, storeId), Map.of()).values().stream()
                .filter(task -> status == null || status.equals(task.status()))
                .sorted(Comparator.comparing(OperationTask::createdAt).reversed())
                .toList();
    }

    @Transactional
    public OperationTask start(String tenantId, String storeId, String taskNo, Long operatorUserId) {
        OperationTask existing = requireTask(tenantId, storeId, taskNo);
        if (!"TODO".equals(existing.status())) {
            throw new IllegalArgumentException("仅TODO任务可开始执行");
        }
        OperationTask updated = existing.withStatus("DOING", "START", operatorUserId, OffsetDateTime.now());
        putTask(updated);
        return updated;
    }

    @Transactional
    public OperationTask complete(String tenantId, String storeId, String taskNo, Long operatorUserId) {
        OperationTask existing = requireTask(tenantId, storeId, taskNo);
        if (!"DOING".equals(existing.status())) {
            throw new IllegalArgumentException("仅DOING任务可完成");
        }
        OperationTask updated = existing.withStatus("DONE", "COMPLETE", operatorUserId, OffsetDateTime.now());
        putTask(updated);
        return updated;
    }

    @Transactional
    public OperationTask close(String tenantId, String storeId, String taskNo, Long operatorUserId) {
        OperationTask existing = requireTask(tenantId, storeId, taskNo);
        if ("DONE".equals(existing.status()) || "CLOSED".equals(existing.status())) {
            throw new IllegalArgumentException("当前状态不可关闭");
        }
        OperationTask updated = existing.withStatus("CLOSED", "CLOSE", operatorUserId, OffsetDateTime.now());
        putTask(updated);
        return updated;
    }

    private void putTask(OperationTask task) {
        taskByNoByKey.computeIfAbsent(key(task.tenantId(), task.storeId()), ignored -> new ConcurrentHashMap<>())
                .put(task.taskNo(), task);
    }

    private TaskRule requireRule(String tenantId, String storeId) {
        TaskRule rule = ruleByKey.get(key(tenantId, storeId));
        if (rule == null) {
            throw new IllegalArgumentException("任务规则不存在，请先配置规则");
        }
        return rule;
    }

    private OperationTask requireTask(String tenantId, String storeId, String taskNo) {
        OperationTask task = taskByNoByKey.getOrDefault(key(tenantId, storeId), Map.of()).get(taskNo);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return task;
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
    }

    public record TaskRule(String tenantId,
                           String storeId,
                           String triggerType,
                           String priority,
                           String ownerRole,
                           String titleTemplate,
                           Integer generateLimit,
                           Long operatorUserId,
                           OffsetDateTime updatedAt) {
    }

    public record GenerateResult(TaskRule rule,
                                 Integer generatedCount,
                                 List<OperationTask> tasks) {
    }

    public record OperationTask(String taskNo,
                                String tenantId,
                                String storeId,
                                String triggerType,
                                String priority,
                                String ownerRole,
                                String title,
                                String status,
                                Long memberId,
                                String source,
                                String lastAction,
                                Long lastOperator,
                                Long createdBy,
                                OffsetDateTime createdAt,
                                OffsetDateTime updatedAt) {
        private OperationTask withStatus(String status,
                                         String action,
                                         Long operator,
                                         OffsetDateTime updatedAt) {
            return new OperationTask(
                    taskNo,
                    tenantId,
                    storeId,
                    triggerType,
                    priority,
                    ownerRole,
                    title,
                    status,
                    memberId,
                    source,
                    action,
                    operator,
                    createdBy,
                    createdAt,
                    updatedAt
            );
        }
    }
}
