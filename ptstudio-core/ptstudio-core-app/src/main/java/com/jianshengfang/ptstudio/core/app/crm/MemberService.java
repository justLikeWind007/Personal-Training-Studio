package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final InMemoryCrmStore store;

    public MemberService(InMemoryCrmStore store) {
        this.store = store;
    }

    public List<InMemoryCrmStore.MemberData> list(String tenantId, String storeId) {
        return store.memberById().values().stream()
                .filter(member -> member.tenantId().equals(tenantId) && member.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryCrmStore.MemberData::id))
                .toList();
    }

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

    public InMemoryCrmStore.MemberData create(CreateMemberCommand command) {
        long id = store.nextMemberId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCrmStore.MemberData member = new InMemoryCrmStore.MemberData(
                id,
                command.tenantId(),
                command.storeId(),
                String.format("M%06d", id),
                command.name(),
                command.mobile(),
                command.levelTag(),
                "ACTIVE",
                null,
                now,
                now,
                now
        );
        store.memberById().put(id, member);
        return member;
    }

    public InMemoryCrmStore.MemberData update(Long id, UpdateMemberCommand command) {
        InMemoryCrmStore.MemberData existing = get(id, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
        InMemoryCrmStore.MemberData updated = new InMemoryCrmStore.MemberData(
                existing.id(),
                existing.tenantId(),
                existing.storeId(),
                existing.memberNo(),
                command.name(),
                command.mobile(),
                command.levelTag(),
                command.status(),
                existing.leadId(),
                existing.joinDate(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        store.memberById().put(id, updated);
        return updated;
    }

    public List<TimelineEvent> timeline(Long id, String tenantId, String storeId) {
        InMemoryCrmStore.MemberData member = get(id, tenantId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("MEMBER_CREATED", "会员建档", member.createdAt()));
        if (member.leadId() != null) {
            events.add(new TimelineEvent("LEAD_CONVERTED", "由线索转化", member.joinDate()));
        }
        return events;
    }

    public record CreateMemberCommand(String tenantId,
                                      String storeId,
                                      String name,
                                      String mobile,
                                      String levelTag) {
    }

    public record UpdateMemberCommand(String tenantId,
                                      String storeId,
                                      String name,
                                      String mobile,
                                      String levelTag,
                                      String status) {
    }

    public record TimelineEvent(String type, String description, OffsetDateTime occurredAt) {
    }
}
