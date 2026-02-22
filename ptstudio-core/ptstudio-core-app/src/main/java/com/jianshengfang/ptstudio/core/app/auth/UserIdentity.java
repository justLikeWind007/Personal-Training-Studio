package com.jianshengfang.ptstudio.core.app.auth;

import java.util.Set;

public record UserIdentity(Long userId,
                           String username,
                           String mobile,
                           String tenantId,
                           String storeId,
                           Set<String> roles) {
}
