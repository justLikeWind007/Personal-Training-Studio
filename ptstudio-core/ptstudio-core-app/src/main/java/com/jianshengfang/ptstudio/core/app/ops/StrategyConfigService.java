package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class StrategyConfigService {

    private final Map<String, StrategyDraft> draftByKey = new ConcurrentHashMap<>();
    private final Map<String, List<StrategyVersion>> historyByKey = new ConcurrentHashMap<>();
    private final Map<String, StrategyVersion> currentByKey = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0L);

    @Transactional
    public StrategyDraft saveDraft(String tenantId,
                                   String storeId,
                                   BigDecimal refundRiskRatioThreshold,
                                   Integer lowAttendanceRateThreshold,
                                   Integer reversedConsumptionDailyThreshold,
                                   String metricCaliber,
                                   String remark,
                                   Long operatorUserId) {
        StrategyDraft draft = new StrategyDraft(
                tenantId,
                storeId,
                refundRiskRatioThreshold,
                lowAttendanceRateThreshold,
                reversedConsumptionDailyThreshold,
                metricCaliber,
                remark,
                operatorUserId,
                OffsetDateTime.now()
        );
        draftByKey.put(key(tenantId, storeId), draft);
        return draft;
    }

    @Transactional
    public StrategyVersion publish(String tenantId, String storeId, Long operatorUserId) {
        StrategyDraft draft = draftByKey.get(key(tenantId, storeId));
        if (draft == null) {
            throw new IllegalArgumentException("策略草稿不存在，请先保存草稿");
        }
        StrategyVersion version = buildVersion(tenantId, storeId, draft, operatorUserId, "PUBLISH", null);
        historyByKey.computeIfAbsent(key(tenantId, storeId), ignored -> new ArrayList<>()).add(version);
        currentByKey.put(key(tenantId, storeId), version);
        return version;
    }

    @Transactional
    public StrategyVersion rollback(String tenantId,
                                    String storeId,
                                    String targetVersion,
                                    Long operatorUserId) {
        List<StrategyVersion> history = historyByKey.getOrDefault(key(tenantId, storeId), List.of());
        StrategyVersion target = history.stream()
                .filter(version -> version.versionNo().equals(targetVersion))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("目标版本不存在"));

        StrategyDraft draft = new StrategyDraft(
                tenantId,
                storeId,
                target.refundRiskRatioThreshold(),
                target.lowAttendanceRateThreshold(),
                target.reversedConsumptionDailyThreshold(),
                target.metricCaliber(),
                "rollback from " + targetVersion,
                operatorUserId,
                OffsetDateTime.now()
        );
        StrategyVersion rolledBack = buildVersion(tenantId, storeId, draft, operatorUserId, "ROLLBACK", targetVersion);
        historyByKey.computeIfAbsent(key(tenantId, storeId), ignored -> new ArrayList<>()).add(rolledBack);
        currentByKey.put(key(tenantId, storeId), rolledBack);
        return rolledBack;
    }

    public StrategyVersion current(String tenantId, String storeId) {
        StrategyVersion current = currentByKey.get(key(tenantId, storeId));
        if (current != null) {
            return current;
        }
        return new StrategyVersion(
                "v0",
                tenantId,
                storeId,
                BigDecimal.valueOf(50),
                60,
                5,
                "DEFAULT",
                "初始化默认策略",
                "DEFAULT",
                null,
                null,
                null
        );
    }

    public List<StrategyVersion> history(String tenantId, String storeId) {
        return historyByKey.getOrDefault(key(tenantId, storeId), List.of())
                .stream()
                .sorted(Comparator.comparing(StrategyVersion::publishedAt).reversed())
                .toList();
    }

    private StrategyVersion buildVersion(String tenantId,
                                         String storeId,
                                         StrategyDraft draft,
                                         Long operatorUserId,
                                         String action,
                                         String sourceVersion) {
        OffsetDateTime now = OffsetDateTime.now();
        String versionNo = "v" + sequence.incrementAndGet();
        return new StrategyVersion(
                versionNo,
                tenantId,
                storeId,
                draft.refundRiskRatioThreshold(),
                draft.lowAttendanceRateThreshold(),
                draft.reversedConsumptionDailyThreshold(),
                draft.metricCaliber(),
                draft.remark(),
                action,
                sourceVersion,
                operatorUserId,
                now
        );
    }

    private String key(String tenantId, String storeId) {
        return tenantId + "|" + storeId;
    }

    public record StrategyDraft(String tenantId,
                                String storeId,
                                BigDecimal refundRiskRatioThreshold,
                                Integer lowAttendanceRateThreshold,
                                Integer reversedConsumptionDailyThreshold,
                                String metricCaliber,
                                String remark,
                                Long operatorUserId,
                                OffsetDateTime updatedAt) {
    }

    public record StrategyVersion(String versionNo,
                                  String tenantId,
                                  String storeId,
                                  BigDecimal refundRiskRatioThreshold,
                                  Integer lowAttendanceRateThreshold,
                                  Integer reversedConsumptionDailyThreshold,
                                  String metricCaliber,
                                  String remark,
                                  String action,
                                  String sourceVersion,
                                  Long operatorUserId,
                                  OffsetDateTime publishedAt) {
    }
}
