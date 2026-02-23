package com.jianshengfang.ptstudio.core.app.ops;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OpsReviewDashboardService {

    private final OperationTaskService operationTaskService;
    private final TouchRecordService touchRecordService;

    public OpsReviewDashboardService(OperationTaskService operationTaskService,
                                     TouchRecordService touchRecordService) {
        this.operationTaskService = operationTaskService;
        this.touchRecordService = touchRecordService;
    }

    public ReviewSnapshot snapshot(String tenantId,
                                   String storeId,
                                   LocalDate dateFrom,
                                   LocalDate dateTo) {
        LocalDate from = dateFrom == null ? LocalDate.now().minusDays(30) : dateFrom;
        LocalDate to = dateTo == null ? LocalDate.now() : dateTo;

        List<OperationTaskService.OperationTask> tasks = operationTaskService.list(tenantId, storeId, null).stream()
                .filter(task -> within(task.createdAt().toLocalDate(), from, to))
                .toList();
        List<TouchRecordService.TouchRecord> touches = touchRecordService.list(tenantId, storeId, null, null).stream()
                .filter(record -> within(record.executedAt().toLocalDate(), from, to))
                .toList();

        int totalTasks = tasks.size();
        int doneTasks = (int) tasks.stream().filter(task -> "DONE".equals(task.status())).count();
        int overdueTasks = (int) tasks.stream()
                .filter(task -> "TODO".equals(task.status()) || "DOING".equals(task.status()))
                .filter(task -> Duration.between(task.createdAt(), OffsetDateTime.now()).toHours() > 24)
                .count();

        int touchCount = touches.size();
        int convertedCount = (int) touches.stream()
                .filter(record -> "CONTACTED".equals(record.result()) || "SUCCESS".equals(record.result()))
                .count();

        BigDecimal completionRate = percent(doneTasks, totalTasks);
        BigDecimal overdueRate = percent(overdueTasks, totalTasks);
        BigDecimal conversionRate = percent(convertedCount, touchCount);

        BigDecimal avgHandleHours = averageHandleHours(tasks);

        return new ReviewSnapshot(
                storeId,
                from,
                to,
                totalTasks,
                doneTasks,
                overdueTasks,
                touchCount,
                convertedCount,
                completionRate,
                overdueRate,
                conversionRate,
                avgHandleHours
        );
    }

    public String exportCsv(String tenantId,
                            String storeId,
                            LocalDate dateFrom,
                            LocalDate dateTo) {
        ReviewSnapshot snapshot = snapshot(tenantId, storeId, dateFrom, dateTo);
        return "storeId,dateFrom,dateTo,totalTasks,doneTasks,overdueTasks,touchCount,convertedCount,completionRate,overdueRate,conversionRate,avgHandleHours\n"
                + snapshot.storeId() + ","
                + snapshot.dateFrom() + ","
                + snapshot.dateTo() + ","
                + snapshot.totalTasks() + ","
                + snapshot.doneTasks() + ","
                + snapshot.overdueTasks() + ","
                + snapshot.touchCount() + ","
                + snapshot.convertedCount() + ","
                + snapshot.completionRate() + ","
                + snapshot.overdueRate() + ","
                + snapshot.conversionRate() + ","
                + snapshot.avgHandleHours() + "\n";
    }

    private boolean within(LocalDate current, LocalDate from, LocalDate to) {
        return !current.isBefore(from) && !current.isAfter(to);
    }

    private BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageHandleHours(List<OperationTaskService.OperationTask> tasks) {
        List<OperationTaskService.OperationTask> doneTasks = tasks.stream()
                .filter(task -> "DONE".equals(task.status()))
                .toList();
        if (doneTasks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long totalHours = doneTasks.stream()
                .mapToLong(task -> Math.max(0, Duration.between(task.createdAt(), task.updatedAt()).toHours()))
                .sum();
        return BigDecimal.valueOf(totalHours)
                .divide(BigDecimal.valueOf(doneTasks.size()), 2, RoundingMode.HALF_UP);
    }

    public record ReviewSnapshot(String storeId,
                                 LocalDate dateFrom,
                                 LocalDate dateTo,
                                 Integer totalTasks,
                                 Integer doneTasks,
                                 Integer overdueTasks,
                                 Integer touchCount,
                                 Integer convertedCount,
                                 BigDecimal completionRate,
                                 BigDecimal overdueRate,
                                 BigDecimal conversionRate,
                                 BigDecimal avgHandleHours) {
    }
}
