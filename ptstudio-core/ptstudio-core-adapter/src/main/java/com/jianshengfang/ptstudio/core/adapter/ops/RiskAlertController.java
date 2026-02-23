package com.jianshengfang.ptstudio.core.adapter.ops;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.ops.RiskAlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops/risk-alerts")
@Validated
public class RiskAlertController {

    private final RiskAlertService riskAlertService;

    public RiskAlertController(RiskAlertService riskAlertService) {
        this.riskAlertService = riskAlertService;
    }

    @GetMapping
    public List<RiskAlertService.RiskAlert> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type) {
        TenantStoreContext context = requireContext();
        return riskAlertService.list(context.tenantId(), context.storeId(), status, type);
    }

    @PostMapping("/{alertNo}/ack")
    @AuditAction(module = "OPS_RISK_ALERT", action = "ACK")
    public RiskAlertService.RiskAlert ack(@PathVariable("alertNo") String alertNo,
                                          @Valid @RequestBody AlertActionRequest request) {
        TenantStoreContext context = requireContext();
        return riskAlertService.ack(context.tenantId(), context.storeId(), alertNo, request.operatorUserId());
    }

    @PostMapping("/{alertNo}/close")
    @AuditAction(module = "OPS_RISK_ALERT", action = "CLOSE")
    public RiskAlertService.RiskAlert close(@PathVariable("alertNo") String alertNo,
                                            @Valid @RequestBody AlertActionRequest request) {
        TenantStoreContext context = requireContext();
        return riskAlertService.close(context.tenantId(), context.storeId(), alertNo, request.operatorUserId());
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type) {
        TenantStoreContext context = requireContext();
        String csv = riskAlertService.exportCsv(context.tenantId(), context.storeId(), status, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_alerts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record AlertActionRequest(@NotNull Long operatorUserId) {
    }
}
