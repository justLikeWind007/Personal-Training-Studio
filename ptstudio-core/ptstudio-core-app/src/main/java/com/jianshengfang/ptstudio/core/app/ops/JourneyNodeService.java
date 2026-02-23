package com.jianshengfang.ptstudio.core.app.ops;

import com.jianshengfang.ptstudio.core.app.attendance.AttendanceService;
import com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore;
import com.jianshengfang.ptstudio.core.app.crm.MemberService;
import com.jianshengfang.ptstudio.core.app.finance.FinanceService;
import com.jianshengfang.ptstudio.core.app.finance.InMemoryFinanceStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class JourneyNodeService {

    private final MemberService memberService;
    private final FinanceService financeService;
    private final AttendanceService attendanceService;
    private final OperationTaskService operationTaskService;

    public JourneyNodeService(MemberService memberService,
                              FinanceService financeService,
                              AttendanceService attendanceService,
                              OperationTaskService operationTaskService) {
        this.memberService = memberService;
        this.financeService = financeService;
        this.attendanceService = attendanceService;
        this.operationTaskService = operationTaskService;
    }

    @Transactional
    public ScanResult scan(String tenantId, String storeId, boolean autoGenerateTask, Long operatorUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<JourneyNode> nodes = new ArrayList<>();

        List<InMemoryCrmStore.MemberData> members = memberService.list(tenantId, storeId, "ACTIVE", null, null);
        Map<Long, OffsetDateTime> lastPaidByMember = financeService.listPayments(tenantId, storeId).stream()
                .filter(payment -> "PAID".equals(payment.payStatus()))
                .collect(Collectors.groupingBy(
                        InMemoryFinanceStore.PaymentData::orderId,
                        Collectors.mapping(InMemoryFinanceStore.PaymentData::paidAt,
                                Collectors.maxBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> financeService.getOrder(entry.getKey(), tenantId, storeId)
                                .map(InMemoryFinanceStore.OrderData::memberId)
                                .orElse(-1L),
                        entry -> entry.getValue().orElse(null),
                        (left, right) -> left == null ? right : (right == null ? left : (left.isAfter(right) ? left : right))
                ));

        Map<Long, OffsetDateTime> lastConsumptionByMember = attendanceService.listConsumptions(tenantId, storeId).stream()
                .filter(consumption -> "CONSUMED".equals(consumption.status()))
                .collect(Collectors.toMap(
                        com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore.ConsumptionData::memberId,
                        com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore.ConsumptionData::consumeTime,
                        (left, right) -> left.isAfter(right) ? left : right
                ));

        for (InMemoryCrmStore.MemberData member : members) {
            if (member.joinDate() != null && ChronoUnit.DAYS.between(member.joinDate(), now) <= 7) {
                nodes.add(node("NEW_CONVERSION_7D", member, "HIGH", "新客7天转化跟进"));
            }

            OffsetDateTime lastPaid = lastPaidByMember.get(member.id());
            if (lastPaid != null) {
                long paidDays = ChronoUnit.DAYS.between(lastPaid, now);
                if (paidDays >= 23 && paidDays <= 30) {
                    nodes.add(node("RENEWAL_WARNING_7D", member, "HIGH", "续费窗口期触达"));
                }
            }

            OffsetDateTime lastConsume = lastConsumptionByMember.get(member.id());
            if (lastConsume != null && ChronoUnit.DAYS.between(lastConsume, now) >= 30) {
                nodes.add(node("SLEEPING_REWAKE_30D", member, "MEDIUM", "沉睡会员唤醒"));
            }
        }

        int generatedTaskCount = 0;
        if (autoGenerateTask) {
            for (int i = 0; i < nodes.size(); i++) {
                JourneyNode node = nodes.get(i);
                OperationTaskService.OperationTask task = operationTaskService.createFromJourney(
                        tenantId,
                        storeId,
                        node.nodeType(),
                        node.memberId(),
                        node.suggestAction() + " - " + node.memberName(),
                        node.priority(),
                        "STORE_MANAGER",
                        operatorUserId
                );
                nodes.set(i, node.withLinkedTaskNo(task.taskNo()));
                generatedTaskCount++;
            }
        }

        return new ScanResult(nodes.size(), generatedTaskCount, nodes);
    }

    private JourneyNode node(String nodeType,
                             InMemoryCrmStore.MemberData member,
                             String priority,
                             String suggestAction) {
        return new JourneyNode(
                "JN_" + nodeType + "_" + member.id(),
                nodeType,
                member.id(),
                member.name(),
                priority,
                suggestAction,
                null
        );
    }

    public record ScanResult(Integer totalNodes,
                             Integer generatedTaskCount,
                             List<JourneyNode> nodes) {
    }

    public record JourneyNode(String nodeNo,
                              String nodeType,
                              Long memberId,
                              String memberName,
                              String priority,
                              String suggestAction,
                              String linkedTaskNo) {
        private JourneyNode withLinkedTaskNo(String linkedTaskNo) {
            return new JourneyNode(nodeNo, nodeType, memberId, memberName, priority, suggestAction, linkedTaskNo);
        }
    }
}
