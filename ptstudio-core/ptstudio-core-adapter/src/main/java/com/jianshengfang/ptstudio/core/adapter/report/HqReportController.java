package com.jianshengfang.ptstudio.core.adapter.report;

import com.jianshengfang.ptstudio.core.app.auth.AuthService;
import com.jianshengfang.ptstudio.core.app.auth.UserIdentity;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.rbac.RbacService;
import com.jianshengfang.ptstudio.core.app.report.HqReportService;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/hq")
@Validated
public class HqReportController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final HqReportService hqReportService;
    private final StoreSettingsService storeSettingsService;
    private final AuthService authService;
    private final RbacService rbacService;

    public HqReportController(HqReportService hqReportService,
                              StoreSettingsService storeSettingsService,
                              AuthService authService,
                              RbacService rbacService) {
        this.hqReportService = hqReportService;
        this.storeSettingsService = storeSettingsService;
        this.authService = authService;
        this.rbacService = rbacService;
    }

    @GetMapping("/reports/overview")
    public HqReportService.HqOverview overview(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        TenantStoreContext context = requireContext();
        return hqReportService.overview(context.tenantId(), resolveAllowedStoreIds(context, authorization));
    }

    @GetMapping("/reports/ranking")
    public List<HqReportService.StoreRankingItem> ranking(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        TenantStoreContext context = requireContext();
        return hqReportService.ranking(context.tenantId(), resolveAllowedStoreIds(context, authorization));
    }

    @GetMapping("/reports/trend")
    public List<HqReportService.TrendPoint> trend(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "days", required = false, defaultValue = "7") Integer days) {
        TenantStoreContext context = requireContext();
        return hqReportService.trend(context.tenantId(), resolveAllowedStoreIds(context, authorization), startDate, days);
    }

    @GetMapping("/reports/stores/{storeId}")
    public HqReportService.StoreDrilldown drilldown(@PathVariable String storeId) {
        TenantStoreContext context = requireContext();
        return hqReportService.drilldown(context.tenantId(), storeId);
    }

    @GetMapping("/metrics/definitions")
    public List<HqReportService.MetricDefinition> metricDefinitions() {
        return hqReportService.listMetricDefinitions();
    }

    @PutMapping("/metrics/definitions/{metricKey}")
    public HqReportService.MetricDefinition updateMetricDefinition(@PathVariable String metricKey,
                                                                   @Valid @RequestBody MetricDefinitionRequest request) {
        return hqReportService.updateMetricDefinition(metricKey, request.displayName(), request.formula(), request.description());
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    private List<String> resolveAllowedStoreIds(TenantStoreContext context, String authorization) {
        List<String> tenantStoreIds = new ArrayList<>(
                storeSettingsService.listByTenant(context.tenantId()).stream().map(store -> store.storeId()).toList()
        );
        if (!tenantStoreIds.contains(context.storeId())) {
            tenantStoreIds.add(context.storeId());
        }
        UserIdentity user = authService.currentUser(extractToken(authorization)).orElse(null);
        if (user == null) {
            return tenantStoreIds;
        }
        Set<String> baseRoles = user.roles() == null ? Set.of() : user.roles();
        rbacService.resolveRoles(user.userId(), baseRoles);
        return rbacService.filterStoreIds(user.userId(), user.storeId(), tenantStoreIds);
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

    public record MetricDefinitionRequest(@NotBlank String displayName,
                                          @NotBlank String formula,
                                          String description) {
    }
}
