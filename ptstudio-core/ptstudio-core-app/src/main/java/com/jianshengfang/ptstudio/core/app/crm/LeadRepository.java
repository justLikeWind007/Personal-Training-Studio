package com.jianshengfang.ptstudio.core.app.crm;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LeadRepository {

    List<InMemoryCrmStore.LeadData> list(String tenantId, String storeId);

    Optional<InMemoryCrmStore.LeadData> get(Long id, String tenantId, String storeId);

    InMemoryCrmStore.LeadData create(String tenantId,
                                     String storeId,
                                     String source,
                                     String name,
                                     String mobile,
                                     Long ownerUserId,
                                     OffsetDateTime nextFollowAt);

    InMemoryCrmStore.LeadData update(Long id,
                                     String tenantId,
                                     String storeId,
                                     String source,
                                     String status,
                                     String name,
                                     String mobile,
                                     Long ownerUserId,
                                     OffsetDateTime nextFollowAt);

    InMemoryCrmStore.LeadFollowData addFollow(Long id,
                                              String tenantId,
                                              String storeId,
                                              String followType,
                                              String content,
                                              OffsetDateTime nextFollowAt,
                                              Long followerUserId);

    List<InMemoryCrmStore.LeadFollowData> listFollows(Long leadId, String tenantId, String storeId);

    InMemoryCrmStore.LeadData markConverted(Long leadId,
                                            String tenantId,
                                            String storeId,
                                            Long convertedMemberId,
                                            OffsetDateTime convertedAt);
}
