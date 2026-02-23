package com.jianshengfang.ptstudio.core.app.ops;

import com.jianshengfang.ptstudio.core.app.attendance.AttendanceService;
import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import com.jianshengfang.ptstudio.core.app.finance.FinanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TargetManagementService {

    private final Map<String, MonthlyTarget> targetByKey = new ConcurrentHashMap<>();
    private final AtomicLong version = new AtomicLong(1L);
    private final FinanceService financeService;
    private final AttendanceService attendanceService;
    private final MemberService memberService;

    public TargetManagementService(FinanceService financeService,
                                   AttendanceService attendanceService,
                                   MemberService memberService) {
        this.financeService = financeService;
        this.attendanceService = attendanceService;
        this.memberService = memberService;
    }

    public MonthlyTarget upsert(String tenantId,
                                String storeId,
                                YearMonth month,
                                BigDecimal revenueTarget,
                                Integer consumptionTarget,
                                Integer newMemberTarget) {
        MonthlyTarget target = new MonthlyTarget(
                tenantId, storeId, month, revenueTarget, consumptionTarget, newMemberTarget,
                version.incrementAndGet(), OffsetDateTime.now()
        );
        targetByKey.put(key(tenantId, storeId, month), target);
        return target;
    }

    public MonthlyTarget getOrDefault(String tenantId, String storeId, YearMonth month) {
        MonthlyTarget existing = targetByKey.get(key(tenantId, storeId, month));
        if (existing != null) {
            return existing;
        }
        return new MonthlyTarget(tenantId, storeId, month, BigDecimal.ZERO, 0, 0, version.get(), null);
    }

    public MonthlyAchievement achievement(String tenantId, String storeId, YearMonth month) {
        MonthlyTarget target = getOrDefault(tenantId, storeId, month);
        BigDecimal actualRevenue = financeService.listPayments(tenantId, storeId).stream()
                .filter(payment -> "PAID".equals(payment.payStatus()))
                .filter(payment -> payment.paidAt() != null && YearMonth.from(payment.paidAt()).equals(month))
                .map(payment -> payment.amount() == null ? BigDecimal.ZERO : payment.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int actualConsumptions = (int) attendanceService.listConsumptions(tenantId, storeId).stream()
                .filter(consumption -> "CONSUMED".equals(consumption.status()))
                .filter(consumption -> consumption.consumeTime() != null && YearMonth.from(consumption.consumeTime()).equals(month))
                .count();
        int actualNewMembers = (int) memberService.list(tenantId, storeId).stream()
                .filter(member -> member.joinDate() != null && YearMonth.from(member.joinDate()).equals(month))
                .count();

        return new MonthlyAchievement(
                month,
                target,
                actualRevenue,
                actualConsumptions,
                actualNewMembers,
                percent(actualRevenue, target.revenueTarget()),
                percent(BigDecimal.valueOf(actualConsumptions), BigDecimal.valueOf(target.consumptionTarget())),
                percent(BigDecimal.valueOf(actualNewMembers), BigDecimal.valueOf(target.newMemberTarget()))
        );
    }

    private BigDecimal percent(BigDecimal actual, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return actual.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);
    }

    private String key(String tenantId, String storeId, YearMonth month) {
        return tenantId + "|" + storeId + "|" + month;
    }

    public record MonthlyTarget(String tenantId,
                                String storeId,
                                YearMonth month,
                                BigDecimal revenueTarget,
                                Integer consumptionTarget,
                                Integer newMemberTarget,
                                Long version,
                                OffsetDateTime updatedAt) {
    }

    public record MonthlyAchievement(YearMonth month,
                                     MonthlyTarget target,
                                     BigDecimal actualRevenue,
                                     Integer actualConsumptions,
                                     Integer actualNewMembers,
                                     BigDecimal revenueRate,
                                     BigDecimal consumptionRate,
                                     BigDecimal newMemberRate) {
    }
}
