package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!mysql")
public class InMemoryMemberRepository implements MemberRepository {

    private final InMemoryCrmStore store;

    public InMemoryMemberRepository(InMemoryCrmStore store) {
        this.store = store;
    }

    @Override
    public List<InMemoryCrmStore.MemberData> list(String tenantId, String storeId) {
        return store.memberById().values().stream()
                .filter(member -> member.tenantId().equals(tenantId) && member.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryCrmStore.MemberData::id))
                .toList();
    }

    @Override
    public Optional<InMemoryCrmStore.MemberData> get(Long id, String tenantId, String storeId) {
        InMemoryCrmStore.MemberData member = store.memberById().get(id);
        if (member == null) {
            return Optional.empty();
        }
        if (!member.tenantId().equals(tenantId) || !member.storeId().equals(storeId)) {
            return Optional.empty();
        }
        return Optional.of(member);
    }

    @Override
    public InMemoryCrmStore.MemberData create(String tenantId, String storeId, String name, String mobile,
                                              String levelTag, Long leadId, OffsetDateTime joinDate) {
        long id = store.nextMemberId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCrmStore.MemberData member = new InMemoryCrmStore.MemberData(
                id,
                tenantId,
                storeId,
                String.format("M%06d", id),
                name,
                mobile,
                levelTag,
                "ACTIVE",
                leadId,
                joinDate,
                now,
                now
        );
        store.memberById().put(id, member);
        return member;
    }

    @Override
    public InMemoryCrmStore.MemberData update(Long id, String tenantId, String storeId, String name,
                                              String mobile, String levelTag, String status) {
        InMemoryCrmStore.MemberData existing = get(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
        InMemoryCrmStore.MemberData updated = new InMemoryCrmStore.MemberData(
                existing.id(),
                existing.tenantId(),
                existing.storeId(),
                existing.memberNo(),
                name,
                mobile,
                levelTag,
                status,
                existing.leadId(),
                existing.joinDate(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        store.memberById().put(id, updated);
        return updated;
    }
}
