package com.jianshengfang.ptstudio.core.app.rbac;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RbacService {

    private final Map<Long, Set<String>> roleByUserId = new ConcurrentHashMap<>();
    private final Map<Long, DataScopeConfig> dataScopeByUserId = new ConcurrentHashMap<>();

    public RbacService() {
        dataScopeByUserId.put(1001L, new DataScopeConfig(DataScopeType.SELF_ONLY, Set.of("store-001")));
        dataScopeByUserId.put(1002L, new DataScopeConfig(DataScopeType.TENANT_ALL, Set.of()));
    }

    public List<String> listSystemRoles() {
        return List.of("STORE_MANAGER", "FINANCE", "SALES", "COACH", "RECEPTION");
    }

    public Set<String> assignRoles(Long userId, Set<String> roles) {
        roleByUserId.put(userId, Set.copyOf(roles));
        return roleByUserId.get(userId);
    }

    public List<RoleAssignment> listAssignments() {
        List<RoleAssignment> result = new ArrayList<>();
        roleByUserId.forEach((userId, roles) -> result.add(new RoleAssignment(userId, roles)));
        return result;
    }

    public DataScopeConfig assignDataScope(Long userId, DataScopeType type, Set<String> storeIds) {
        Set<String> normalizedStoreIds = (storeIds == null) ? Set.of() : Set.copyOf(storeIds);
        if (type == DataScopeType.STORE_ASSIGNED && normalizedStoreIds.isEmpty()) {
            throw new IllegalArgumentException("STORE_ASSIGNED 必须配置门店范围");
        }
        DataScopeConfig config = new DataScopeConfig(type, normalizedStoreIds);
        dataScopeByUserId.put(userId, config);
        return config;
    }

    public DataScopeConfig getDataScope(Long userId) {
        return dataScopeByUserId.getOrDefault(userId, new DataScopeConfig(DataScopeType.SELF_ONLY, Set.of()));
    }

    public boolean canAccessStore(Long userId, String userStoreId, String requestStoreId) {
        DataScopeConfig config = getDataScope(userId);
        return switch (config.type()) {
            case TENANT_ALL -> true;
            case STORE_ASSIGNED -> config.storeIds().contains(requestStoreId);
            case SELF_ONLY -> userStoreId != null && userStoreId.equals(requestStoreId);
        };
    }

    public List<String> filterStoreIds(Long userId, String userStoreId, List<String> tenantStoreIds) {
        DataScopeConfig config = getDataScope(userId);
        return switch (config.type()) {
            case TENANT_ALL -> tenantStoreIds;
            case STORE_ASSIGNED -> tenantStoreIds.stream().filter(config.storeIds()::contains).toList();
            case SELF_ONLY -> tenantStoreIds.stream().filter(id -> id.equals(userStoreId)).toList();
        };
    }

    public record RoleAssignment(Long userId, Set<String> roles) {
    }

    public enum DataScopeType {
        TENANT_ALL,
        STORE_ASSIGNED,
        SELF_ONLY
    }

    public record DataScopeConfig(DataScopeType type, Set<String> storeIds) {
    }
}
