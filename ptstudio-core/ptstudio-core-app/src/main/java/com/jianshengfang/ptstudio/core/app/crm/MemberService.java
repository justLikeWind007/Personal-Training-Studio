package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<InMemoryCrmStore.MemberData> list(String tenantId, String storeId) {
        return memberRepository.list(tenantId, storeId);
    }

    public Optional<InMemoryCrmStore.MemberData> get(Long id, String tenantId, String storeId) {
        return memberRepository.get(id, tenantId, storeId);
    }

    public InMemoryCrmStore.MemberData create(CreateMemberCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        return memberRepository.create(
                command.tenantId(),
                command.storeId(),
                command.name(),
                command.mobile(),
                command.levelTag(),
                null,
                now
        );
    }

    public InMemoryCrmStore.MemberData update(Long id, UpdateMemberCommand command) {
        return memberRepository.update(
                id,
                command.tenantId(),
                command.storeId(),
                command.name(),
                command.mobile(),
                command.levelTag(),
                command.status()
        );
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
