package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!mysql")
public class InMemoryLeadRepository implements LeadRepository {

    private final InMemoryCrmStore store;

    public InMemoryLeadRepository(InMemoryCrmStore store) {
        this.store = store;
    }

    @Override
    public List<InMemoryCrmStore.LeadData> list(String tenantId, String storeId) {
        return store.leadById().values().stream()
                .filter(lead -> lead.tenantId().equals(tenantId) && lead.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryCrmStore.LeadData::id))
                .toList();
    }

    @Override
    public Optional<InMemoryCrmStore.LeadData> get(Long id, String tenantId, String storeId) {
        InMemoryCrmStore.LeadData lead = store.leadById().get(id);
        if (lead == null) {
            return Optional.empty();
        }
        if (!lead.tenantId().equals(tenantId) || !lead.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(lead);
    }

    @Override
    public InMemoryCrmStore.LeadData create(String tenantId, String storeId, String source, String name,
                                            String mobile, Long ownerUserId, OffsetDateTime nextFollowAt) {
        long id = store.nextLeadId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCrmStore.LeadData lead = new InMemoryCrmStore.LeadData(
                id, tenantId, storeId, source, "NEW", name, mobile, ownerUserId,
                null, nextFollowAt, now, now
        );
        store.leadById().put(id, lead);
        return lead;
    }

    @Override
    public InMemoryCrmStore.LeadData update(Long id, String tenantId, String storeId, String source,
                                            String status, String name, String mobile,
                                            Long ownerUserId, OffsetDateTime nextFollowAt) {
        InMemoryCrmStore.LeadData existing = get(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        InMemoryCrmStore.LeadData updated = new InMemoryCrmStore.LeadData(
                existing.id(), existing.tenantId(), existing.storeId(), source, status, name, mobile,
                ownerUserId, existing.lastFollowAt(), nextFollowAt, existing.createdAt(), OffsetDateTime.now()
        );
        store.leadById().put(id, updated);
        return updated;
    }

    @Override
    public InMemoryCrmStore.LeadFollowData addFollow(Long id, String tenantId, String storeId,
                                                     String followType, String content,
                                                     OffsetDateTime nextFollowAt, Long followerUserId) {
        InMemoryCrmStore.LeadData lead = get(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        long followId = store.nextFollowId();
        InMemoryCrmStore.LeadFollowData follow = new InMemoryCrmStore.LeadFollowData(
                followId, lead.id(), lead.tenantId(), lead.storeId(), followType,
                content, nextFollowAt, followerUserId, OffsetDateTime.now()
        );
        store.follows(lead.id()).add(follow);

        InMemoryCrmStore.LeadData touched = new InMemoryCrmStore.LeadData(
                lead.id(), lead.tenantId(), lead.storeId(), lead.source(), "FOLLOWING", lead.name(),
                lead.mobile(), lead.ownerUserId(), follow.createdAt(), nextFollowAt,
                lead.createdAt(), OffsetDateTime.now()
        );
        store.leadById().put(lead.id(), touched);
        return follow;
    }

    @Override
    public List<InMemoryCrmStore.LeadFollowData> listFollows(Long leadId, String tenantId, String storeId) {
        get(leadId, tenantId, storeId).orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        return List.copyOf(store.follows(leadId));
    }

    @Override
    public InMemoryCrmStore.LeadData markConverted(Long leadId, String tenantId, String storeId,
                                                   Long convertedMemberId, OffsetDateTime convertedAt) {
        InMemoryCrmStore.LeadData lead = get(leadId, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        InMemoryCrmStore.LeadData converted = new InMemoryCrmStore.LeadData(
                lead.id(), lead.tenantId(), lead.storeId(), lead.source(), "CONVERTED",
                lead.name(), lead.mobile(), lead.ownerUserId(), lead.lastFollowAt(),
                lead.nextFollowAt(), lead.createdAt(), convertedAt
        );
        store.leadById().put(lead.id(), converted);
        return converted;
    }
}
