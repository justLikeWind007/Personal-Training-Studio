package com.jianshengfang.ptstudio.core.adapter.crm;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.crm.InMemoryCrmStore;
import com.jianshengfang.ptstudio.core.app.crm.MemberService;
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

import java.util.List;

@RestController
@RequestMapping("/api/members")
@Validated
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<InMemoryCrmStore.MemberData> list() {
        TenantStoreContext context = requireContext();
        return memberService.list(context.tenantId(), context.storeId());
    }

    @PostMapping
    @AuditAction(module = "CRM_MEMBER", action = "CREATE")
    public InMemoryCrmStore.MemberData create(@Valid @RequestBody CreateMemberRequest request) {
        TenantStoreContext context = requireContext();
        return memberService.create(new MemberService.CreateMemberCommand(
                context.tenantId(),
                context.storeId(),
                request.name(),
                request.mobile(),
                request.levelTag()
        ));
    }

    @GetMapping("/{id}")
    public InMemoryCrmStore.MemberData detail(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return memberService.get(id, context.tenantId(), context.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "CRM_MEMBER", action = "UPDATE")
    public InMemoryCrmStore.MemberData update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateMemberRequest request) {
        TenantStoreContext context = requireContext();
        return memberService.update(id, new MemberService.UpdateMemberCommand(
                context.tenantId(),
                context.storeId(),
                request.name(),
                request.mobile(),
                request.levelTag(),
                request.status()
        ));
    }

    @GetMapping("/{id}/timeline")
    public List<MemberService.TimelineEvent> timeline(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return memberService.timeline(id, context.tenantId(), context.storeId());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateMemberRequest(@NotBlank String name,
                                      @NotBlank String mobile,
                                      @NotBlank String levelTag) {
    }

    public record UpdateMemberRequest(@NotBlank String name,
                                      @NotBlank String mobile,
                                      @NotBlank String levelTag,
                                      @NotBlank String status) {
    }
}
