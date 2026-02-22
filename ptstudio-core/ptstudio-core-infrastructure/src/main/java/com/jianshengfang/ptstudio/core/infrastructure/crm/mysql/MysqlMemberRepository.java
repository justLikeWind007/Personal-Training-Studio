package com.jianshengfang.ptstudio.core.infrastructure.crm.mysql;

import com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore;
import com.jianshengfang.ptstudio.core.app.crm.MemberRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("mysql")
public class MysqlMemberRepository implements MemberRepository {

    private final MysqlMemberMapper memberMapper;

    public MysqlMemberRepository(MysqlMemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Override
    public List<InMemoryCrmStore.MemberData> list(String tenantId, String storeId) {
        return memberMapper.list(toLong(tenantId), toLong(storeId)).stream().map(this::toMemberData).toList();
    }

    @Override
    public Optional<InMemoryCrmStore.MemberData> get(Long id, String tenantId, String storeId) {
        MysqlMemberPo po = memberMapper.get(id, toLong(tenantId), toLong(storeId));
        return Optional.ofNullable(po).map(this::toMemberData);
    }

    @Override
    public InMemoryCrmStore.MemberData create(String tenantId, String storeId, String name,
                                              String mobile, String levelTag,
                                              Long leadId, OffsetDateTime joinDate) {
        MysqlMemberPo po = new MysqlMemberPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setMemberNo("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        po.setMemberName(name);
        po.setMobileHash(mobile);
        po.setJoinDate(joinDate.toLocalDate());
        po.setLevelTag(levelTag);
        po.setStatus("ACTIVE");
        po.setLeadId(leadId);
        memberMapper.insert(po);
        return toMemberData(memberMapper.get(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public InMemoryCrmStore.MemberData update(Long id, String tenantId, String storeId,
                                              String name, String mobile, String levelTag, String status) {
        MysqlMemberPo existing = memberMapper.get(id, toLong(tenantId), toLong(storeId));
        if (existing == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        existing.setMemberName(name);
        existing.setMobileHash(mobile);
        existing.setLevelTag(levelTag);
        existing.setStatus(status);
        memberMapper.update(existing);
        return toMemberData(memberMapper.get(id, existing.getTenantId(), existing.getStoreId()));
    }

    private InMemoryCrmStore.MemberData toMemberData(MysqlMemberPo po) {
        OffsetDateTime joinDate = po.getJoinDate() == null ? OffsetDateTime.now() : po.getJoinDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        return new InMemoryCrmStore.MemberData(
                po.getId(),
                String.valueOf(po.getTenantId()),
                String.valueOf(po.getStoreId()),
                po.getMemberNo(),
                po.getMemberName(),
                po.getMobileHash(),
                po.getLevelTag(),
                po.getStatus(),
                po.getLeadId(),
                joinDate,
                po.getCreatedAt(),
                po.getUpdatedAt()
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
