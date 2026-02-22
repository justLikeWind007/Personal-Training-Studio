package com.jianshengfang.ptstudio.core.adapter.rbac;

import com.jianshengfang.ptstudio.core.app.rbac.RbacService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/rbac")
@Validated
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/roles")
    public List<String> listRoles() {
        return rbacService.listSystemRoles();
    }

    @PostMapping("/users/{id}/roles")
    public RoleAssignmentResponse assignRoles(@PathVariable("id") Long userId,
                                              @Valid @RequestBody RoleAssignmentRequest request) {
        Set<String> assigned = rbacService.assignRoles(userId, request.roles());
        return new RoleAssignmentResponse(userId, assigned);
    }

    public record RoleAssignmentRequest(@NotEmpty Set<String> roles) {
    }

    public record RoleAssignmentResponse(Long userId, Set<String> roles) {
    }
}
