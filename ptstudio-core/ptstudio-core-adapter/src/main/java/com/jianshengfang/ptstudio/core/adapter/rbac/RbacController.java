package com.jianshengfang.ptstudio.core.adapter.rbac;

import com.jianshengfang.ptstudio.core.app.auth.AuthService;
import com.jianshengfang.ptstudio.core.app.auth.UserIdentity;
import com.jianshengfang.ptstudio.core.app.rbac.RbacService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/rbac")
@Validated
public class RbacController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RbacService rbacService;
    private final AuthService authService;

    public RbacController(RbacService rbacService, AuthService authService) {
        this.rbacService = rbacService;
        this.authService = authService;
    }

    @GetMapping("/roles")
    public List<String> listRoles() {
        return rbacService.listSystemRoles();
    }

    @GetMapping("/roles/catalog")
    public List<RoleCatalogItemResponse> listRoleCatalog(@RequestParam(name = "level", required = false) String level) {
        RbacService.RoleLevel roleLevel = level == null || level.isBlank()
                ? null
                : RbacService.RoleLevel.valueOf(level);
        return rbacService.listRoleDefinitions(roleLevel).stream()
                .map(item -> new RoleCatalogItemResponse(item.roleKey(), item.displayName(), item.level().name()))
                .toList();
    }

    @PostMapping("/users/{id}/roles")
    public RoleAssignmentResponse assignRoles(@PathVariable("id") Long userId,
                                              @Valid @RequestBody RoleAssignmentRequest request) {
        Set<String> assigned = rbacService.assignRoles(userId, request.roles());
        return new RoleAssignmentResponse(userId, assigned);
    }

    @PostMapping("/users/{id}/data-scope")
    public DataScopeResponse assignDataScope(@PathVariable("id") Long userId,
                                             @Valid @RequestBody DataScopeRequest request) {
        RbacService.DataScopeConfig config = rbacService.assignDataScope(
                userId,
                RbacService.DataScopeType.valueOf(request.scopeType()),
                request.storeIds()
        );
        return new DataScopeResponse(userId, config.type().name(), config.storeIds());
    }

    @GetMapping("/users/{id}/data-scope")
    public DataScopeResponse getDataScope(@PathVariable("id") Long userId) {
        RbacService.DataScopeConfig config = rbacService.getDataScope(userId);
        return new DataScopeResponse(userId, config.type().name(), config.storeIds());
    }

    @GetMapping("/permissions/catalog")
    public PermissionCatalogResponse permissionCatalog() {
        RbacService.PermissionCatalog catalog = rbacService.permissionCatalog();
        return new PermissionCatalogResponse(catalog.menuKeys(), catalog.buttonKeys());
    }

    @GetMapping("/roles/{roleKey}/permissions")
    public RolePermissionResponse getRolePermissions(@PathVariable String roleKey) {
        RbacService.RolePermissionConfig config = rbacService.getRolePermissions(roleKey);
        return new RolePermissionResponse(config.roleKey(), config.menuKeys(), config.buttonKeys());
    }

    @PutMapping("/roles/{roleKey}/permissions")
    public RolePermissionResponse updateRolePermissions(@PathVariable String roleKey,
                                                        @Valid @RequestBody RolePermissionRequest request) {
        RbacService.RolePermissionConfig config = rbacService.configureRolePermissions(
                roleKey, request.menuKeys(), request.buttonKeys());
        return new RolePermissionResponse(config.roleKey(), config.menuKeys(), config.buttonKeys());
    }

    @GetMapping("/users/{id}/permissions")
    public UserPermissionResponse getUserPermissions(@PathVariable("id") Long userId,
                                                     @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        UserIdentity current = authService.currentUser(extractToken(authorization)).orElse(null);
        Set<String> baseRoles = current != null && userId.equals(current.userId()) ? current.roles() : Set.of();
        RbacService.UserPermissionSnapshot snapshot = rbacService.getUserPermissions(userId, baseRoles);
        return new UserPermissionResponse(
                snapshot.userId(),
                snapshot.roles(),
                snapshot.menuKeys(),
                snapshot.buttonKeys(),
                snapshot.version()
        );
    }

    public record RoleAssignmentRequest(@NotEmpty Set<String> roles) {
    }

    public record RoleAssignmentResponse(Long userId, Set<String> roles) {
    }

    public record RoleCatalogItemResponse(String roleKey,
                                          String displayName,
                                          String level) {
    }

    public record DataScopeRequest(@NotBlank String scopeType,
                                   Set<String> storeIds) {
    }

    public record DataScopeResponse(Long userId,
                                    String scopeType,
                                    Set<String> storeIds) {
    }

    public record PermissionCatalogResponse(Set<String> menuKeys,
                                            Set<String> buttonKeys) {
    }

    public record RolePermissionRequest(Set<String> menuKeys,
                                        Set<String> buttonKeys) {
    }

    public record RolePermissionResponse(String roleKey,
                                         Set<String> menuKeys,
                                         Set<String> buttonKeys) {
    }

    public record UserPermissionResponse(Long userId,
                                         Set<String> roles,
                                         Set<String> menuKeys,
                                         Set<String> buttonKeys,
                                         Long version) {
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return authorization;
    }
}
