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

    public record RoleAssignment(Long userId, Set<String> roles) {
    }
}
