package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    private final InMemoryCrmStore store;

    public LeadService(InMemoryCrmStore store) {
        this.store = store;
    }

    public List<InMemoryCrmStore.LeadData> list(String tenantId, String storeId) {
        return store.leadById().values().stream()
                .filter(lead -> lead.tenantId().equals(tenantId) && lead.storeId().equals(storeId))
                .sorted(Comparator.comparing(InMemoryCrmStore.LeadData::id))
                .toList();
    }

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

    public InMemoryCrmStore.LeadData create(CreateLeadCommand command) {
        long id = store.nextLeadId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCrmStore.LeadData lead = new InMemoryCrmStore.LeadData(
                id,
                command.tenantId(),
                command.storeId(),
                command.source(),
                "NEW",
                command.name(),
                command.mobile(),
                command.ownerUserId(),
                null,
                command.nextFollowAt(),
                now,
                now
        );
        store.leadById().put(id, lead);
        return lead;
    }

    public InMemoryCrmStore.LeadData update(Long id, UpdateLeadCommand command) {
        InMemoryCrmStore.LeadData existing = get(id, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        InMemoryCrmStore.LeadData updated = new InMemoryCrmStore.LeadData(
                existing.id(),
                existing.tenantId(),
                existing.storeId(),
                command.source(),
                command.status(),
                command.name(),
                command.mobile(),
                command.ownerUserId(),
                existing.lastFollowAt(),
                command.nextFollowAt(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        store.leadById().put(id, updated);
        return updated;
    }

    public InMemoryCrmStore.LeadFollowData addFollow(Long id, AddFollowCommand command) {
        InMemoryCrmStore.LeadData lead = get(id, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        long followId = store.nextFollowId();
        InMemoryCrmStore.LeadFollowData follow = new InMemoryCrmStore.LeadFollowData(
                followId,
                lead.id(),
                lead.tenantId(),
                lead.storeId(),
                command.followType(),
                command.content(),
                command.nextFollowAt(),
                command.followerUserId(),
                OffsetDateTime.now()
        );
        store.follows(lead.id()).add(follow);

        InMemoryCrmStore.LeadData touched = new InMemoryCrmStore.LeadData(
                lead.id(),
                lead.tenantId(),
                lead.storeId(),
                lead.source(),
                "FOLLOWING",
                lead.name(),
                lead.mobile(),
                lead.ownerUserId(),
                follow.createdAt(),
                command.nextFollowAt(),
                lead.createdAt(),
                OffsetDateTime.now()
        );
        store.leadById().put(lead.id(), touched);
        return follow;
    }

    public InMemoryCrmStore.MemberData convertMember(Long id, ConvertMemberCommand command) {
        InMemoryCrmStore.LeadData lead = get(id, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        long memberId = store.nextMemberId();
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCrmStore.MemberData member = new InMemoryCrmStore.MemberData(
                memberId,
                lead.tenantId(),
                lead.storeId(),
                String.format("M%06d", memberId),
                lead.name(),
                lead.mobile(),
                "STANDARD",
                "ACTIVE",
                lead.id(),
                now,
                now,
                now
        );
        store.memberById().put(memberId, member);

        InMemoryCrmStore.LeadData converted = new InMemoryCrmStore.LeadData(
                lead.id(),
                lead.tenantId(),
                lead.storeId(),
                lead.source(),
                "CONVERTED",
                lead.name(),
                lead.mobile(),
                lead.ownerUserId(),
                lead.lastFollowAt(),
                lead.nextFollowAt(),
                lead.createdAt(),
                now
        );
        store.leadById().put(lead.id(), converted);
        return member;
    }

    public List<InMemoryCrmStore.LeadFollowData> listFollows(Long id, String tenantId, String storeId) {
        get(id, tenantId, storeId).orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        return List.copyOf(store.follows(id));
    }

    public record CreateLeadCommand(String tenantId,
                                    String storeId,
                                    String source,
                                    String name,
                                    String mobile,
                                    Long ownerUserId,
                                    OffsetDateTime nextFollowAt) {
    }

    public record UpdateLeadCommand(String tenantId,
                                    String storeId,
                                    String source,
                                    String status,
                                    String name,
                                    String mobile,
                                    Long ownerUserId,
                                    OffsetDateTime nextFollowAt) {
    }

    public record AddFollowCommand(String tenantId,
                                   String storeId,
                                   String followType,
                                   String content,
                                   OffsetDateTime nextFollowAt,
                                   Long followerUserId) {
    }

    public record ConvertMemberCommand(String tenantId, String storeId) {
    }
}
