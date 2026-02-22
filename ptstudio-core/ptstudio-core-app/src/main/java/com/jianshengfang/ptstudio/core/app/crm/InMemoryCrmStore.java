package com.jianshengfang.ptstudio.core.app.crm;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryCrmStore {

    private final AtomicLong leadIdGenerator = new AtomicLong(1);
    private final AtomicLong followIdGenerator = new AtomicLong(1);
    private final AtomicLong memberIdGenerator = new AtomicLong(1);

    private final Map<Long, LeadData> leadById = new ConcurrentHashMap<>();
    private final Map<Long, List<LeadFollowData>> followsByLeadId = new ConcurrentHashMap<>();
    private final Map<Long, MemberData> memberById = new ConcurrentHashMap<>();

    public long nextLeadId() {
        return leadIdGenerator.getAndIncrement();
    }

    public long nextFollowId() {
        return followIdGenerator.getAndIncrement();
    }

    public long nextMemberId() {
        return memberIdGenerator.getAndIncrement();
    }

    public Map<Long, LeadData> leadById() {
        return leadById;
    }

    public Map<Long, List<LeadFollowData>> followsByLeadId() {
        return followsByLeadId;
    }

    public Map<Long, MemberData> memberById() {
        return memberById;
    }

    public record LeadData(Long id,
                           String tenantId,
                           String storeId,
                           String source,
                           String status,
                           String name,
                           String mobile,
                           Long ownerUserId,
                           OffsetDateTime lastFollowAt,
                           OffsetDateTime nextFollowAt,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {
    }

    public record LeadFollowData(Long id,
                                 Long leadId,
                                 String tenantId,
                                 String storeId,
                                 String followType,
                                 String content,
                                 OffsetDateTime nextFollowAt,
                                 Long followerUserId,
                                 OffsetDateTime createdAt) {
    }

    public record MemberData(Long id,
                             String tenantId,
                             String storeId,
                             String memberNo,
                             String name,
                             String mobile,
                             String levelTag,
                             String status,
                             Long leadId,
                             OffsetDateTime joinDate,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
    }

    public List<LeadFollowData> follows(Long leadId) {
        return followsByLeadId.computeIfAbsent(leadId, key -> new ArrayList<>());
    }
}
