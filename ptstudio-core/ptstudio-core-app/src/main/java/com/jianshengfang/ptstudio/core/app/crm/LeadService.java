package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final MemberRepository memberRepository;

    public LeadService(LeadRepository leadRepository, MemberRepository memberRepository) {
        this.leadRepository = leadRepository;
        this.memberRepository = memberRepository;
    }

    public List<InMemoryCrmStore.LeadData> list(String tenantId, String storeId) {
        return leadRepository.list(tenantId, storeId);
    }

    public Optional<InMemoryCrmStore.LeadData> get(Long id, String tenantId, String storeId) {
        return leadRepository.get(id, tenantId, storeId);
    }

    public InMemoryCrmStore.LeadData create(CreateLeadCommand command) {
        return leadRepository.create(
                command.tenantId(),
                command.storeId(),
                command.source(),
                command.name(),
                command.mobile(),
                command.ownerUserId(),
                command.nextFollowAt()
        );
    }

    public InMemoryCrmStore.LeadData update(Long id, UpdateLeadCommand command) {
        return leadRepository.update(
                id,
                command.tenantId(),
                command.storeId(),
                command.source(),
                command.status(),
                command.name(),
                command.mobile(),
                command.ownerUserId(),
                command.nextFollowAt()
        );
    }

    public InMemoryCrmStore.LeadFollowData addFollow(Long id, AddFollowCommand command) {
        return leadRepository.addFollow(
                id,
                command.tenantId(),
                command.storeId(),
                command.followType(),
                command.content(),
                command.nextFollowAt(),
                command.followerUserId()
        );
    }

    public InMemoryCrmStore.MemberData convertMember(Long id, ConvertMemberCommand command) {
        InMemoryCrmStore.LeadData lead = get(id, command.tenantId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
        OffsetDateTime now = OffsetDateTime.now();
        InMemoryCrmStore.MemberData member = memberRepository.create(
                lead.tenantId(),
                lead.storeId(),
                lead.name(),
                lead.mobile(),
                "STANDARD",
                lead.id(),
                now
        );
        leadRepository.markConverted(lead.id(), lead.tenantId(), lead.storeId(), member.id(), now);
        return member;
    }

    public List<InMemoryCrmStore.LeadFollowData> listFollows(Long id, String tenantId, String storeId) {
        return leadRepository.listFollows(id, tenantId, storeId);
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
