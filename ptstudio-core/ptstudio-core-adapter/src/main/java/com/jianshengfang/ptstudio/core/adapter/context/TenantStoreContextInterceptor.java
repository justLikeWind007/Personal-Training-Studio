package com.jianshengfang.ptstudio.core.adapter.context;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.audit.AuditLogService;
import com.jianshengfang.ptstudio.core.app.auth.AuthService;
import com.jianshengfang.ptstudio.core.app.auth.UserIdentity;
import com.jianshengfang.ptstudio.core.app.rbac.RbacService;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class TenantStoreContextInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String STORE_HEADER = "X-Store-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StoreSettingsService storeSettingsService;
    private final AuthService authService;
    private final RbacService rbacService;
    private final AuditLogService auditLogService;

    public TenantStoreContextInterceptor(StoreSettingsService storeSettingsService,
                                         AuthService authService,
                                         RbacService rbacService,
                                         AuditLogService auditLogService) {
        this.storeSettingsService = storeSettingsService;
        this.authService = authService;
        this.rbacService = rbacService;
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        String storeId = request.getHeader(STORE_HEADER);
        String requestId = firstNonBlank(request.getHeader(REQUEST_ID_HEADER), UUID.randomUUID().toString());
        String traceId = firstNonBlank(
                request.getHeader(TRACE_ID_HEADER),
                request.getHeader(B3_TRACE_ID_HEADER),
                requestId
        );
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        if (tenantId == null || tenantId.isBlank() || storeId == null || storeId.isBlank()) {
            throw new IllegalArgumentException("缺少租户门店上下文请求头");
        }
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);
        MDC.put("tenantId", tenantId);
        MDC.put("storeId", storeId);
        validateDataScope(request, tenantId, storeId);
        if (isWriteMethod(request.getMethod()) && shouldValidateWritableStore(request.getRequestURI())
                && !storeSettingsService.isStoreWritable(tenantId, storeId)) {
            throw new IllegalArgumentException("门店已停用，禁止写操作");
        }
        TenantStoreContextHolder.set(new TenantStoreContext(tenantId, storeId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantStoreContextHolder.clear();
        MDC.remove("requestId");
        MDC.remove("traceId");
        MDC.remove("tenantId");
        MDC.remove("storeId");
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean shouldValidateWritableStore(String uri) {
        if (uri == null) {
            return true;
        }
        return !uri.startsWith("/api/admin/stores")
                && !uri.startsWith("/api/auth/");
    }

    private void validateDataScope(HttpServletRequest request, String tenantId, String storeId) {
        if (!request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/")) {
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return;
        }
        String token = extractToken(authorization);
        UserIdentity user = authService.currentUser(token).orElse(null);
        if (user == null) {
            return;
        }
        if (!tenantId.equals(user.tenantId())) {
            auditLogService.log("SECURITY", "DATA_SCOPE_DENY",
                    "tenant_mismatch userId=" + user.userId() + ", userTenant=" + user.tenantId() + ", requestTenant=" + tenantId,
                    user.username());
            throw new IllegalArgumentException("无权访问该租户数据");
        }
        if (!rbacService.canAccessStore(user.userId(), user.storeId(), storeId)) {
            RbacService.DataScopeConfig scope = rbacService.getDataScope(user.userId());
            auditLogService.log("SECURITY", "DATA_SCOPE_DENY",
                    "userId=" + user.userId() + ", scope=" + scope.type() + ", requestStore=" + storeId,
                    user.username());
            throw new IllegalArgumentException("无权访问该门店数据");
        }
    }

    private String extractToken(String authorization) {
        if (authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return authorization;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
