package com.jianshengfang.ptstudio.core.app.rbac;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RbacService {

    private final Map<Long, Set<String>> roleByUserId = new ConcurrentHashMap<>();
    private final Map<Long, DataScopeConfig> dataScopeByUserId = new ConcurrentHashMap<>();
    private final Map<String, RolePermissionConfig> permissionByRole = new ConcurrentHashMap<>();
    private final Map<String, RoleDefinition> roleDefinitionByKey = new ConcurrentHashMap<>();
    private final AtomicLong permissionVersion = new AtomicLong(1L);

    public RbacService() {
        dataScopeByUserId.put(1001L, new DataScopeConfig(DataScopeType.SELF_ONLY, Set.of("store-001")));
        dataScopeByUserId.put(1002L, new DataScopeConfig(DataScopeType.TENANT_ALL, Set.of()));
        permissionByRole.put("STORE_MANAGER", new RolePermissionConfig(
                "STORE_MANAGER",
                Set.of("dashboard", "stores", "rbac", "crm", "schedule", "finance", "report"),
                Set.of("store.create", "store.status", "rbac.assign", "rbac.dataScope", "refund.approve", "refund.reject")
        ));
        permissionByRole.put("FINANCE", new RolePermissionConfig(
                "FINANCE",
                Set.of("finance", "reconciliation", "report"),
                Set.of("refund.approve", "refund.reject", "reconcile.export")
        ));
        permissionByRole.put("SALES", new RolePermissionConfig(
                "SALES",
                Set.of("crm", "member", "reservation"),
                Set.of("lead.create", "lead.follow", "member.create", "reservation.create")
        ));
        permissionByRole.put("COACH", new RolePermissionConfig(
                "COACH",
                Set.of("schedule", "reservation"),
                Set.of("slot.create", "reservation.view")
        ));
        permissionByRole.put("RECEPTION", new RolePermissionConfig(
                "RECEPTION",
                Set.of("checkin", "reservation"),
                Set.of("checkin.create", "consumption.create", "consumption.reverse")
        ));
        permissionByRole.put("HQ_ADMIN", new RolePermissionConfig(
                "HQ_ADMIN",
                Set.of("dashboard", "stores", "rbac", "finance", "reconciliation", "report"),
                Set.of("store.create", "store.status", "rbac.assign", "rbac.dataScope", "reconcile.export")
        ));
        permissionByRole.put("HQ_FINANCE_ANALYST", new RolePermissionConfig(
                "HQ_FINANCE_ANALYST",
                Set.of("finance", "reconciliation", "report"),
                Set.of("refund.approve", "refund.reject", "reconcile.export")
        ));
        permissionByRole.put("HQ_AUDITOR", new RolePermissionConfig(
                "HQ_AUDITOR",
                Set.of("reconciliation", "report"),
                Set.of("reconcile.export")
        ));
        roleDefinitionByKey.put("STORE_MANAGER", new RoleDefinition("STORE_MANAGER", "门店店长", RoleLevel.STORE));
        roleDefinitionByKey.put("FINANCE", new RoleDefinition("FINANCE", "门店财务", RoleLevel.STORE));
        roleDefinitionByKey.put("SALES", new RoleDefinition("SALES", "门店销售", RoleLevel.STORE));
        roleDefinitionByKey.put("COACH", new RoleDefinition("COACH", "门店教练", RoleLevel.STORE));
        roleDefinitionByKey.put("RECEPTION", new RoleDefinition("RECEPTION", "门店前台", RoleLevel.STORE));
        roleDefinitionByKey.put("HQ_ADMIN", new RoleDefinition("HQ_ADMIN", "总部管理员", RoleLevel.HEADQUARTER));
        roleDefinitionByKey.put("HQ_FINANCE_ANALYST", new RoleDefinition("HQ_FINANCE_ANALYST", "总部财务分析", RoleLevel.HEADQUARTER));
        roleDefinitionByKey.put("HQ_AUDITOR", new RoleDefinition("HQ_AUDITOR", "总部稽核员", RoleLevel.HEADQUARTER));
    }

    public List<String> listSystemRoles() {
        return List.of(
                "STORE_MANAGER", "FINANCE", "SALES", "COACH", "RECEPTION",
                "HQ_ADMIN", "HQ_FINANCE_ANALYST", "HQ_AUDITOR"
        );
    }

    public List<RoleDefinition> listRoleDefinitions(RoleLevel level) {
        return listSystemRoles().stream()
                .map(roleKey -> roleDefinitionByKey.getOrDefault(
                        roleKey, new RoleDefinition(roleKey, roleKey, RoleLevel.STORE)))
                .filter(definition -> level == null || definition.level() == level)
                .toList();
    }

    public Set<String> assignRoles(Long userId, Set<String> roles) {
        roleByUserId.put(userId, Set.copyOf(roles));
        return roleByUserId.get(userId);
    }

    public Set<String> resolveRoles(Long userId, Set<String> baseRoles) {
        Set<String> assigned = roleByUserId.get(userId);
        if (assigned != null && !assigned.isEmpty()) {
            return assigned;
        }
        return baseRoles == null ? Set.of() : Set.copyOf(baseRoles);
    }

    public List<RoleAssignment> listAssignments() {
        List<RoleAssignment> result = new ArrayList<>();
        roleByUserId.forEach((userId, roles) -> result.add(new RoleAssignment(userId, roles)));
        return result;
    }

    public RolePermissionConfig configureRolePermissions(String roleKey, Set<String> menuKeys, Set<String> buttonKeys) {
        if (roleKey == null || roleKey.isBlank()) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        RolePermissionConfig config = new RolePermissionConfig(
                roleKey,
                menuKeys == null ? Set.of() : Set.copyOf(menuKeys),
                buttonKeys == null ? Set.of() : Set.copyOf(buttonKeys)
        );
        permissionByRole.put(roleKey, config);
        permissionVersion.incrementAndGet();
        return config;
    }

    public RolePermissionConfig getRolePermissions(String roleKey) {
        return permissionByRole.getOrDefault(roleKey, new RolePermissionConfig(roleKey, Set.of(), Set.of()));
    }

    public UserPermissionSnapshot getUserPermissions(Long userId, Set<String> baseRoles) {
        Set<String> roles = resolveRoles(userId, baseRoles);
        Set<String> menus = new LinkedHashSet<>();
        Set<String> buttons = new LinkedHashSet<>();
        for (String role : roles) {
            RolePermissionConfig config = getRolePermissions(role);
            menus.addAll(config.menuKeys());
            buttons.addAll(config.buttonKeys());
        }
        return new UserPermissionSnapshot(userId, roles, Set.copyOf(menus), Set.copyOf(buttons), permissionVersion.get());
    }

    public PermissionCatalog permissionCatalog() {
        return new PermissionCatalog(
                Set.of("dashboard", "stores", "rbac", "crm", "schedule", "finance", "reconciliation", "report", "member", "reservation", "checkin"),
                Set.of("store.create", "store.status", "rbac.assign", "rbac.dataScope", "refund.approve", "refund.reject",
                        "reconcile.export", "lead.create", "lead.follow", "member.create", "reservation.create",
                        "slot.create", "reservation.view", "checkin.create", "consumption.create", "consumption.reverse")
        );
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

    public record RolePermissionConfig(String roleKey,
                                       Set<String> menuKeys,
                                       Set<String> buttonKeys) {
    }

    public record UserPermissionSnapshot(Long userId,
                                         Set<String> roles,
                                         Set<String> menuKeys,
                                         Set<String> buttonKeys,
                                         Long version) {
    }

    public record PermissionCatalog(Set<String> menuKeys, Set<String> buttonKeys) {
    }

    public record RoleDefinition(String roleKey, String displayName, RoleLevel level) {
    }

    public enum DataScopeType {
        TENANT_ALL,
        STORE_ASSIGNED,
        SELF_ONLY
    }

    public enum RoleLevel {
        HEADQUARTER,
        STORE
    }

    public record DataScopeConfig(DataScopeType type, Set<String> storeIds) {
    }
}
