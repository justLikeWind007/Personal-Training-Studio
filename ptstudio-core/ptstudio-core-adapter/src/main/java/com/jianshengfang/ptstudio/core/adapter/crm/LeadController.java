package com.jianshengfang.ptstudio.core.adapter.crm;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore;
import com.jianshengfang.ptstudio.core.app.crm.LeadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
@Validated
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public List<InMemoryCrmStore.LeadData> list() {
        TenantStoreContext context = requireContext();
        return leadService.list(context.tenantId(), context.storeId());
    }

    @PostMapping
    @AuditAction(module = "CRM_LEAD", action = "CREATE")
    public InMemoryCrmStore.LeadData create(@Valid @RequestBody CreateLeadRequest request) {
        TenantStoreContext context = requireContext();
        return leadService.create(new LeadService.CreateLeadCommand(
                context.tenantId(),
                context.storeId(),
                request.source(),
                request.name(),
                request.mobile(),
                request.ownerUserId(),
                request.nextFollowAt()
        ));
    }

    @GetMapping("/{id}")
    public InMemoryCrmStore.LeadData detail(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return leadService.get(id, context.tenantId(), context.storeId())
                .orElseThrow(() -> new IllegalArgumentException("线索不存在"));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "CRM_LEAD", action = "UPDATE")
    public InMemoryCrmStore.LeadData update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateLeadRequest request) {
        TenantStoreContext context = requireContext();
        return leadService.update(id, new LeadService.UpdateLeadCommand(
                context.tenantId(),
                context.storeId(),
                request.source(),
                request.status(),
                request.name(),
                request.mobile(),
                request.ownerUserId(),
                request.nextFollowAt()
        ));
    }

    @PostMapping("/{id}/follows")
    @AuditAction(module = "CRM_LEAD", action = "FOLLOW")
    public InMemoryCrmStore.LeadFollowData addFollow(@PathVariable Long id,
                                                      @Valid @RequestBody FollowRequest request) {
        TenantStoreContext context = requireContext();
        return leadService.addFollow(id, new LeadService.AddFollowCommand(
                context.tenantId(),
                context.storeId(),
                request.followType(),
                request.content(),
                request.nextFollowAt(),
                request.followerUserId()
        ));
    }

    @PostMapping("/{id}/convert-member")
    @AuditAction(module = "CRM_LEAD", action = "CONVERT_MEMBER")
    public InMemoryCrmStore.MemberData convertMember(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return leadService.convertMember(id, new LeadService.ConvertMemberCommand(
                context.tenantId(),
                context.storeId()
        ));
    }

    @GetMapping("/{id}/follows")
    public List<InMemoryCrmStore.LeadFollowData> follows(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return leadService.listFollows(id, context.tenantId(), context.storeId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateLeadRequest(@NotBlank String source,
                                    @NotBlank String name,
                                    @NotBlank String mobile,
                                    Long ownerUserId,
                                    OffsetDateTime nextFollowAt) {
    }

    public record UpdateLeadRequest(@NotBlank String source,
                                    @NotBlank String status,
                                    @NotBlank String name,
                                    @NotBlank String mobile,
                                    Long ownerUserId,
                                    OffsetDateTime nextFollowAt) {
    }

    public record FollowRequest(@NotBlank String followType,
                                @NotBlank String content,
                                OffsetDateTime nextFollowAt,
                                Long followerUserId) {
    }
}
