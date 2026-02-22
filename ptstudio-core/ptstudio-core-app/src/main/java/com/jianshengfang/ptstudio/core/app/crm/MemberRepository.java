package com.jianshengfang.ptstudio.core.app.crm;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    List<InMemoryCrmStore.MemberData> list(String tenantId, String storeId);

    Optional<InMemoryCrmStore.MemberData> get(Long id, String tenantId, String storeId);

    InMemoryCrmStore.MemberData create(String tenantId,
                                       String storeId,
                                       String name,
                                       String mobile,
                                       String levelTag,
                                       Long leadId,
                                       OffsetDateTime joinDate);

    InMemoryCrmStore.MemberData update(Long id,
                                       String tenantId,
                                       String storeId,
                                       String name,
                                       String mobile,
                                       String levelTag,
                                       String status);
}
