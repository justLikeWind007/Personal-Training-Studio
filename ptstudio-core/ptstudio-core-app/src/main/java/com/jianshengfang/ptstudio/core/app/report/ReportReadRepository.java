package com.jianshengfang.ptstudio.core.app.report;

import java.math.BigDecimal;

public interface ReportReadRepository {

    long countDistinctMembers(String tenantId, String storeId);

    long countReservations(String tenantId, String storeId);

    long countCanceledReservations(String tenantId, String storeId);

    long countCheckins(String tenantId, String storeId);

    long countConsumedRecords(String tenantId, String storeId);

    BigDecimal sumPaidRevenue(String tenantId, String storeId);

    BigDecimal sumOrderAmount(String tenantId, String storeId);

    BigDecimal sumApprovedRefundAmount(String tenantId, String storeId);

    BigDecimal sumCommissionAmount(String tenantId, String storeId);
}
