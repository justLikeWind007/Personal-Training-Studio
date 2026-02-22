package com.jianshengfang.ptstudio.core.infrastructure.crm.mysql;

import com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore;
import com.jianshengfang.ptstudio.core.app.crm.LeadRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("mysql")
public class MysqlLeadRepository implements LeadRepository {

    private final MysqlLeadMapper leadMapper;

    public MysqlLeadRepository(MysqlLeadMapper leadMapper) {
        this.leadMapper = leadMapper;
    }

    @Override
    public List<InMemoryCrmStore.LeadData> list(String tenantId, String storeId) {
        return leadMapper.list(toLong(tenantId), toLong(storeId)).stream().map(this::toLeadData).toList();
    }

    @Override
    public Optional<InMemoryCrmStore.LeadData> get(Long id, String tenantId, String storeId) {
        MysqlLeadPo po = leadMapper.get(id, toLong(tenantId), toLong(storeId));
        return Optional.ofNullable(po).map(this::toLeadData);
    }

    @Override
    public InMemoryCrmStore.LeadData create(String tenantId, String storeId, String source, String name,
                                            String mobile, Long ownerUserId, OffsetDateTime nextFollowAt) {
        MysqlLeadPo po = new MysqlLeadPo();
        po.setTenantId(toLong(tenantId));
        po.setStoreId(toLong(storeId));
        po.setLeadNo("L" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        po.setSource(source);
        po.setStatus("NEW");
        po.setLeadName(name);
        po.setMobileHash(mobile);
        po.setOwnerUserId(ownerUserId == null ? 0L : ownerUserId);
        po.setNextFollowAt(nextFollowAt);
        leadMapper.insert(po);
        return toLeadData(leadMapper.get(po.getId(), po.getTenantId(), po.getStoreId()));
    }

    @Override
    public InMemoryCrmStore.LeadData update(Long id, String tenantId, String storeId, String source,
                                            String status, String name, String mobile,
                                            Long ownerUserId, OffsetDateTime nextFollowAt) {
        MysqlLeadPo existing = leadMapper.get(id, toLong(tenantId), toLong(storeId));
        if (existing == null) {
            throw new IllegalArgumentException("线索不存在");
        }
        existing.setSource(source);
        existing.setStatus(status);
        existing.setLeadName(name);
        existing.setMobileHash(mobile);
        existing.setOwnerUserId(ownerUserId == null ? 0L : ownerUserId);
        existing.setNextFollowAt(nextFollowAt);
        leadMapper.update(existing);
        return toLeadData(leadMapper.get(id, existing.getTenantId(), existing.getStoreId()));
    }

    @Override
    public InMemoryCrmStore.LeadFollowData addFollow(Long id, String tenantId, String storeId,
                                                     String followType, String content,
                                                     OffsetDateTime nextFollowAt, Long followerUserId) {
        MysqlLeadPo existing = leadMapper.get(id, toLong(tenantId), toLong(storeId));
        if (existing == null) {
            throw new IllegalArgumentException("线索不存在");
        }

        MysqlLeadFollowPo followPo = new MysqlLeadFollowPo();
        followPo.setTenantId(existing.getTenantId());
        followPo.setStoreId(existing.getStoreId());
        followPo.setLeadId(id);
        followPo.setFollowType(followType);
        followPo.setContent(content);
        followPo.setNextFollowAt(nextFollowAt);
        followPo.setFollowerUserId(followerUserId == null ? 0L : followerUserId);
        leadMapper.insertFollow(followPo);
        leadMapper.updateAfterFollow(id, existing.getTenantId(), existing.getStoreId(), OffsetDateTime.now(), nextFollowAt);

        List<MysqlLeadFollowPo> follows = leadMapper.listFollows(id, existing.getTenantId(), existing.getStoreId());
        MysqlLeadFollowPo latest = follows.get(follows.size() - 1);
        return toLeadFollowData(latest, tenantId, storeId);
    }

    @Override
    public List<InMemoryCrmStore.LeadFollowData> listFollows(Long leadId, String tenantId, String storeId) {
        MysqlLeadPo existing = leadMapper.get(leadId, toLong(tenantId), toLong(storeId));
        if (existing == null) {
            throw new IllegalArgumentException("线索不存在");
        }
        return leadMapper.listFollows(leadId, existing.getTenantId(), existing.getStoreId())
                .stream().map(po -> toLeadFollowData(po, tenantId, storeId)).toList();
    }

    @Override
    public InMemoryCrmStore.LeadData markConverted(Long leadId, String tenantId, String storeId,
                                                   Long convertedMemberId, OffsetDateTime convertedAt) {
        MysqlLeadPo existing = leadMapper.get(leadId, toLong(tenantId), toLong(storeId));
        if (existing == null) {
            throw new IllegalArgumentException("线索不存在");
        }
        leadMapper.markConverted(leadId, existing.getTenantId(), existing.getStoreId(), convertedMemberId);
        return toLeadData(leadMapper.get(leadId, existing.getTenantId(), existing.getStoreId()));
    }

    private InMemoryCrmStore.LeadData toLeadData(MysqlLeadPo po) {
        return new InMemoryCrmStore.LeadData(
                po.getId(),
                String.valueOf(po.getTenantId()),
                String.valueOf(po.getStoreId()),
                po.getSource(),
                po.getStatus(),
                po.getLeadName(),
                po.getMobileHash(),
                po.getOwnerUserId(),
                po.getLastFollowAt(),
                po.getNextFollowAt(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    private InMemoryCrmStore.LeadFollowData toLeadFollowData(MysqlLeadFollowPo po, String tenantId, String storeId) {
        return new InMemoryCrmStore.LeadFollowData(
                po.getId(),
                po.getLeadId(),
                tenantId,
                storeId,
                po.getFollowType(),
                po.getContent(),
                po.getNextFollowAt(),
                po.getFollowerUserId(),
                po.getCreatedAt()
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
