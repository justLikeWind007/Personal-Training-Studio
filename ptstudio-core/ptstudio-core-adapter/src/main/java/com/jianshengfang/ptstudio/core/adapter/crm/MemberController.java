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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    public List<InMemoryCrmStore.MemberData> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "levelTag", required = false) String levelTag,
            @RequestParam(name = "keyword", required = false) String keyword) {
        TenantStoreContext context = requireContext();
        return memberService.list(context.tenantId(), context.storeId(), status, levelTag, keyword);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "levelTag", required = false) String levelTag,
            @RequestParam(name = "keyword", required = false) String keyword) {
        TenantStoreContext context = requireContext();
        List<InMemoryCrmStore.MemberData> members = memberService.list(
                context.tenantId(), context.storeId(), status, levelTag, keyword);
        StringBuilder csv = new StringBuilder("memberNo,name,mobile,levelTag,status,joinDate\n");
        for (InMemoryCrmStore.MemberData member : members) {
            csv.append(member.memberNo()).append(",")
                    .append(member.name()).append(",")
                    .append(member.mobile()).append(",")
                    .append(member.levelTag()).append(",")
                    .append(member.status()).append(",")
                    .append(member.joinDate()).append("\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"members.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
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
    public InMemoryCrmStore.MemberData detail(@PathVariable("id") Long id) {
        TenantStoreContext context = requireContext();
        return memberService.get(id, context.tenantId(), context.storeId())
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "CRM_MEMBER", action = "UPDATE")
    public InMemoryCrmStore.MemberData update(@PathVariable("id") Long id,
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
    public List<MemberService.TimelineEvent> timeline(@PathVariable("id") Long id) {
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
