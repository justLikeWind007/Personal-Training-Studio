package com.jianshengfang.ptstudio.core.infrastructure.attendance.mysql;

import com.jianshengfang.ptstudio.core.app.attendance.AttendanceRepository;
import com.jianshengfang.ptstudio.core.app.attendance.InMemoryAttendanceStore;
import com.jianshengfang.ptstudio.core.app.crm.MemberRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlAttendanceRepository implements AttendanceRepository {

    private final MysqlAttendanceMapper mapper;
    private final MemberRepository memberRepository;

    public MysqlAttendanceRepository(MysqlAttendanceMapper mapper, MemberRepository memberRepository) {
        this.mapper = mapper;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean existsCheckedInReservation(String tenantId, String storeId, Long reservationId) {
        return mapper.countCheckinByReservation(toLong(tenantId), toLong(storeId), reservationId) > 0;
    }

    @Override
    public InMemoryAttendanceStore.CheckinData createCheckin(String tenantId, String storeId, Long reservationId,
                                                             Long memberId, String checkinChannel,
                                                             Long operatorUserId, OffsetDateTime checkinTime) {
        MysqlAttendancePo.CheckinPo po = new MysqlAttendancePo.CheckinPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setCheckinNo("CK" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        po.setReservationId(reservationId);
        po.setMemberId(memberId);
        po.setCheckinTime(checkinTime);
        po.setCheckinChannel(checkinChannel);
        po.setOperatorUserId(operatorUserId);
        po.setStatus("DONE");
        po.setCreatedAt(checkinTime);
        mapper.insertCheckin(po);
        return toCheckin(po, tenantId, storeId);
    }

    @Override
    public List<InMemoryAttendanceStore.CheckinData> listCheckins(String tenantId, String storeId) {
        return mapper.listCheckins(toLong(tenantId), toLong(storeId)).stream()
                .map(po -> toCheckin(po, tenantId, storeId))
                .toList();
    }

    @Override
    public Optional<InMemoryAttendanceStore.ConsumptionData> getConsumptionByIdemKey(String tenantId,
                                                                                      String storeId,
                                                                                      String idemKey) {
        return Optional.ofNullable(mapper.getConsumptionByIdemKey(toLong(tenantId), toLong(storeId), idemKey))
                .map(po -> toConsumption(po, tenantId, storeId));
    }

    @Override
    public InMemoryAttendanceStore.ConsumptionData createConsumption(String tenantId, String storeId,
                                                                     Long reservationId, Long memberId,
                                                                     Integer sessionsDelta, String idemKey,
                                                                     Long operatorUserId, OffsetDateTime consumeTime) {
        long memberPackageAccountId = memberRepository.get(memberId, tenantId, storeId).map(m -> m.id()).orElse(0L);

        MysqlAttendancePo.ConsumptionPo po = new MysqlAttendancePo.ConsumptionPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setConsumptionNo("CS" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        po.setReservationId(reservationId);
        po.setMemberPackageAccountId(memberPackageAccountId);
        po.setActionType("DEDUCT");
        po.setSessionsDelta(sessionsDelta);
        po.setConsumeTime(consumeTime);
        po.setOperatorUserId(operatorUserId);
        po.setIdemKey(idemKey);
        po.setStatus("SUCCESS");
        po.setCreatedAt(consumeTime);
        mapper.insertConsumption(po);
        return toConsumption(po, tenantId, storeId);
    }

    @Override
    public List<InMemoryAttendanceStore.ConsumptionData> listConsumptions(String tenantId, String storeId) {
        return mapper.listConsumptions(toLong(tenantId), toLong(storeId)).stream()
                .map(po -> toConsumption(po, tenantId, storeId))
                .toList();
    }

    @Override
    public Optional<InMemoryAttendanceStore.ConsumptionData> getConsumption(Long consumptionId,
                                                                            String tenantId,
                                                                            String storeId) {
        return Optional.ofNullable(mapper.getConsumption(consumptionId, toLong(tenantId), toLong(storeId)))
                .map(po -> toConsumption(po, tenantId, storeId));
    }

    @Override
    public InMemoryAttendanceStore.ConsumptionData updateConsumptionStatus(Long consumptionId,
                                                                           String tenantId,
                                                                           String storeId,
                                                                           String status,
                                                                           Long operatorUserId,
                                                                           OffsetDateTime updatedAt) {
        String dbStatus = "CONSUMED".equals(status) ? "SUCCESS" : status;
        mapper.updateConsumptionStatus(consumptionId, toLong(tenantId), toLong(storeId), dbStatus, operatorUserId);
        MysqlAttendancePo.ConsumptionPo po = mapper.getConsumption(consumptionId, toLong(tenantId), toLong(storeId));
        return toConsumption(po, tenantId, storeId);
    }

    @Override
    public InMemoryAttendanceStore.ApprovalRequestData createApprovalRequest(String tenantId,
                                                                              String storeId,
                                                                              String bizType,
                                                                              Long bizId,
                                                                              String reason,
                                                                              Long submittedBy,
                                                                              OffsetDateTime submittedAt) {
        MysqlAttendancePo.ApprovalPo po = new MysqlAttendancePo.ApprovalPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setBizType(bizType);
        po.setBizId(bizId);
        po.setStatus("PENDING");
        po.setReason(reason);
        po.setSubmittedBy(submittedBy);
        po.setSubmittedAt(submittedAt);
        mapper.insertApproval(po);
        MysqlAttendancePo.ApprovalPo latest = mapper.getApproval(po.getId(), po.getTenantId(), po.getStoreId());
        return toApproval(latest, tenantId, storeId);
    }

    @Override
    public Optional<InMemoryAttendanceStore.ApprovalRequestData> getApprovalRequestByBiz(String tenantId,
                                                                                           String storeId,
                                                                                           String bizType,
                                                                                           Long bizId) {
        return Optional.ofNullable(mapper.getApprovalByBiz(toLong(tenantId), toLong(storeId), bizType, bizId))
                .map(po -> toApproval(po, tenantId, storeId));
    }

    @Override
    public Optional<InMemoryAttendanceStore.ApprovalRequestData> getApprovalRequest(Long id,
                                                                                     String tenantId,
                                                                                     String storeId) {
        return Optional.ofNullable(mapper.getApproval(id, toLong(tenantId), toLong(storeId)))
                .map(po -> toApproval(po, tenantId, storeId));
    }

    @Override
    public List<InMemoryAttendanceStore.ApprovalRequestData> listApprovalRequests(String tenantId,
                                                                                   String storeId,
                                                                                   String bizType,
                                                                                   String status) {
        return mapper.listApprovals(toLong(tenantId), toLong(storeId), bizType, status).stream()
                .map(po -> toApproval(po, tenantId, storeId))
                .toList();
    }

    @Override
    public InMemoryAttendanceStore.ApprovalRequestData updateApprovalRequest(String tenantId,
                                                                             String storeId,
                                                                             Long id,
                                                                             String status,
                                                                             Long approvedBy,
                                                                             String rejectReason,
                                                                             OffsetDateTime approvedAt) {
        mapper.updateApproval(id, toLong(tenantId), toLong(storeId), status, approvedBy, approvedAt, rejectReason);
        MysqlAttendancePo.ApprovalPo po = mapper.getApproval(id, toLong(tenantId), toLong(storeId));
        return toApproval(po, tenantId, storeId);
    }

    private InMemoryAttendanceStore.CheckinData toCheckin(MysqlAttendancePo.CheckinPo po, String tenantId, String storeId) {
        return new InMemoryAttendanceStore.CheckinData(
                po.getId(), po.getReservationId(), po.getMemberId(), tenantId, storeId,
                po.getCheckinChannel(), po.getOperatorUserId(),
                "DONE".equals(po.getStatus()) ? "CHECKED_IN" : po.getStatus(),
                po.getCheckinTime(), po.getCreatedAt()
        );
    }

    private InMemoryAttendanceStore.ConsumptionData toConsumption(MysqlAttendancePo.ConsumptionPo po,
                                                                  String tenantId,
                                                                  String storeId) {
        String status = "SUCCESS".equals(po.getStatus()) ? "CONSUMED" : po.getStatus();
        return new InMemoryAttendanceStore.ConsumptionData(
                po.getId(), po.getReservationId(), po.getMemberPackageAccountId(), po.getSessionsDelta(),
                tenantId, storeId, po.getIdemKey(), po.getOperatorUserId(),
                status, po.getConsumeTime(), po.getCreatedAt(), po.getCreatedAt()
        );
    }

    private InMemoryAttendanceStore.ApprovalRequestData toApproval(MysqlAttendancePo.ApprovalPo po,
                                                                   String tenantId,
                                                                   String storeId) {
        return new InMemoryAttendanceStore.ApprovalRequestData(
                po.getId(),
                tenantId,
                storeId,
                po.getBizType(),
                po.getBizId(),
                po.getStatus(),
                po.getReason(),
                po.getSubmittedBy(),
                po.getSubmittedAt(),
                po.getApprovedBy(),
                po.getApprovedAt(),
                po.getRejectReason()
        );
    }

    private long toLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1L;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 1L;
        }
        return Long.parseLong(digits);
    }
}
