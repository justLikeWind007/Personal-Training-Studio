package com.jianshengfang.ptstudio.core.app.commission;

import com.jianshengfang.ptstudio.core.app.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommissionService {

    private final CommissionRepository commissionRepository;
    private final ScheduleRepository scheduleRepository;

    public CommissionService(CommissionRepository commissionRepository, ScheduleRepository scheduleRepository) {
        this.commissionRepository = commissionRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<InMemoryCommissionStore.CommissionRuleData> listRules(String tenantId, String storeId) {
        return commissionRepository.listRules(tenantId, storeId);
    }

    public InMemoryCommissionStore.CommissionRuleData createRule(CreateRuleCommand command) {
        if (command.ratio().compareTo(BigDecimal.ZERO) <= 0 || command.ratio().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("提成比例必须在(0,1]区间");
        }

        int nextVersion = commissionRepository.nextRuleVersion(command.tenantId(), command.storeId());
        OffsetDateTime now = OffsetDateTime.now();
        return commissionRepository.createRule(
                command.tenantId(),
                command.storeId(),
                command.name(),
                command.calcMode(),
                command.ratio(),
                nextVersion,
                command.effectiveFrom(),
                command.effectiveTo(),
                now
        );
    }

    public List<InMemoryCommissionStore.CommissionStatementData> generateStatements(GenerateStatementCommand command) {
        InMemoryCommissionStore.CommissionRuleData rule = commissionRepository
                .getRule(command.ruleId(), command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("提成规则不存在"));

        List<Long> coachIds = scheduleRepository.listCoaches(command.tenantId(), command.storeId()).stream()
                .map(c -> c.id())
                .sorted()
                .collect(Collectors.toList());

        if (coachIds.isEmpty()) {
            throw new IllegalArgumentException("当前门店暂无教练，无法生成提成单");
        }

        return coachIds.stream().map(coachId -> {
            var existing = commissionRepository.findStatement(
                    command.tenantId(), command.storeId(), command.statementMonth(), coachId);
            if (existing.isPresent()) {
                return existing.get();
            }

            BigDecimal grossAmount = BigDecimal.valueOf(1000 + coachId * 100);
            BigDecimal commissionAmount = grossAmount.multiply(rule.ratio())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            OffsetDateTime now = OffsetDateTime.now();
            return commissionRepository.createStatement(
                    command.tenantId(),
                    command.storeId(),
                    command.statementMonth(),
                    coachId,
                    rule.id(),
                    grossAmount,
                    commissionAmount,
                    now
            );
        }).toList();
    }

    public InMemoryCommissionStore.CommissionStatementData lockStatement(Long id, String tenantId, String storeId) {
        InMemoryCommissionStore.CommissionStatementData statement = commissionRepository.getStatement(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("提成单不存在"));
        if (!statement.status().equals("GENERATED")) {
            throw new IllegalArgumentException("当前状态不可锁账");
        }

        return commissionRepository.lockStatement(id, tenantId, storeId, OffsetDateTime.now());
    }

    public List<InMemoryCommissionStore.CommissionStatementData> listStatements(String tenantId,
                                                                                 String storeId,
                                                                                 YearMonth statementMonth,
                                                                                 String status) {
        return commissionRepository.listStatements(tenantId, storeId, statementMonth, status);
    }

    public record CreateRuleCommand(String tenantId,
                                    String storeId,
                                    String name,
                                    String calcMode,
                                    BigDecimal ratio,
                                    OffsetDateTime effectiveFrom,
                                    OffsetDateTime effectiveTo) {
    }

    public record GenerateStatementCommand(String tenantId,
                                           String storeId,
                                           YearMonth statementMonth,
                                           Long ruleId) {
    }
}
